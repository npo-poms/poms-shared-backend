/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v3.filter;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Slf4j
public class MediaPropertiesFilters {

    private static final List<String> ignoreFields = Arrays.asList(
        "mid",
        "midRef",
        "type",
        "avType",
        "sortDate",
        "isEmbeddable",
        "parent"
    );

    private static final List<String> ignoreSignatures = Arrays.asList(
        //"Ljava/util/Date;",
        //"Ljava/lang/String;"
        "Z", // boolean
        "B", // byte
        "C", // char
        "S", // short
        "I", // int
        "J", // long
        "F", // float
        "D" // double
    );

    private static final List<String> knownProperties = new ArrayList<>();

    private static boolean instrumented = false;


    public static synchronized  void instrument() {
        if (! instrumented) {
            instrument(
                "nl.vpro.domain.media.support.PublishableObject",
                "nl.vpro.domain.media.MediaObject",
                "nl.vpro.domain.media.Program",
                "nl.vpro.domain.media.Group",
                "nl.vpro.domain.media.Segment"
            );
            instrumentScheduleEvents("nl.vpro.domain.media.Schedule");
            instrumented = true;
            log.info("Instrumented media properties " + getKnownProperties());
        } else {
            log.warn("Instrumented already");
        }
    }

    static List<String> getKnownProperties() {
        return Collections.unmodifiableList(knownProperties);
    }
    static boolean isInstrumented() {
        return instrumented;
    }


    private static void instrument(String... classNames) {
        try {
            ClassPool cp = ClassPool.getDefault();
            cp.childFirstLookup = true;

            ClassLoader cl = MediaPropertiesFilters.class.getClassLoader();
            cp.appendClassPath(new LoaderClassPath(cl));

            CtClass[] ctClasses = cp.get(classNames);

            for (final CtClass ctClass : ctClasses) {
                try {
                    ctClass.instrument(new ExprEditor() {
                        @Override
                        public void edit(FieldAccess f) throws CannotCompileException {
                            try {
                                /* Ignore static fields / methods */
                                if ((f.getField().getModifiers() & Modifier.STATIC) == 0) {
                                    String fieldName = f.getFieldName();
                                    if (!knownProperties.contains(fieldName)) {
                                        knownProperties.add(fieldName);
                                    } else {
                                        log.debug("Found {} more than once!", fieldName);
                                    }
                                    if (ignoreSignatures.contains(f.getSignature()) || ignoreFields.contains(fieldName)) {
                                        log.debug("Never filtering {}", fieldName);
                                        return;
                                    }

                                    if (("Ljava/util/SortedSet;".equals(f.getSignature()) || "Ljava/util/Set;".equals(f.getSignature())) && f.isReader()) {
                                        log.debug("Instrumenting Set {}", fieldName);
                                        if ("titles".equals(fieldName)) {
                                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v3.filter.FilteredSortedTitleSet.wrapTitles(\"" + f.getFieldName() + "\", $proceed($$));");
                                        } else if ("descriptions".equals(fieldName)) {
                                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v3.filter.FilteredSortedDescriptionSet.wrapDescriptions(\"" + f.getFieldName() + "\", $proceed($$));");
                                        } else {
                                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v3.filter.FilteredSortedSet.wrap(\"" + f.getFieldName() + "\", $proceed($$));");
                                        }
                                    } else if ("Ljava/util/List;".equals(f.getSignature()) && f.isReader()) {
                                        log.debug("Instrumenting List {}", fieldName);
                                        f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v3.filter.FilteredList.wrap(\"" + f.getFieldName() + "\", $proceed($$));");
                                    } else {
                                        log.debug("Instrumenting {}", fieldName);
                                        f.replace("$_ = $proceed($$) == null ? null : ($r)nl.vpro.api.rs.v3.filter.FilteredObject.wrap(\"" + f.getFieldName() + "\", $proceed($$)).value();");
                                    }
                                }
                            } catch (RuntimeException | NotFoundException | CannotCompileException wtf) {
                                log.error("During instrumentation of '" + ctClass + "." + f.getFieldName() + "' : " + wtf.getMessage(), wtf);
                            }
                        }
                    });

                    ctClass.toClass();
                } catch (RuntimeException wtf ){
                    log.error(wtf.getMessage());
                }
            }
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static void instrumentScheduleEvents(String... classNames) {
        try {
            ClassPool cp = new ClassPool(ClassPool.getDefault());
            cp.childFirstLookup = true;

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            cp.insertClassPath(new LoaderClassPath(cl));

            CtClass[] ctClasses = cp.get(classNames);

            for(CtClass ctClass : ctClasses) {
                try {
                    ctClass.instrument(new ExprEditor() {
                        @Override
                        public void edit(FieldAccess f) throws CannotCompileException {
                            if ("Ljava/util/SortedSet;".equals(f.getSignature()) && f.isReader() && f.getFieldName().equals("scheduleEvents")) {
                                log.debug("Instrumenting ScheduleEvents for {} on field {}", f.getClassName(), f.getFieldName());
                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v3.filter.ScheduleEventViewSortedSet.wrap($proceed($$));");
                            }

                        }
                    });
                    ctClass.toClass();
                } catch (RuntimeException wtf) {
                    log.warn(wtf.getMessage());
                }
            }
        } catch(CannotCompileException | NotFoundException e) {
            // schedule event stuff is optional
            // It is not used in the player api, so it does have ScheduleEventView.
            log.warn(e.getMessage());
        }
    }

}
