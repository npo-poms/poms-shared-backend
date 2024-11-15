/*
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.filter;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import lombok.extern.log4j.Log4j2;

import java.util.*;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
@Log4j2
public class MediaPropertiesFilters {

    private static final Set<String> ignoreFields = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "mid",
        "midRef",
        "type",
        "avType",
        "sortInstant",
        "isEmbeddable",
        "parent"
    )));

    private static final Set<String> ignoreSignatures = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "Z", // boolean
        "B", // byte
        "C", // char
        "S", // short
        "I", // int
        "J", // long
        "F", // float
        "D" // double
    )));


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
            log.debug("Instrumented already");
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
                log.debug("Instrumenting {}", ctClass.getName());
                try {
                    ctClass.instrument(new ExprEditor() {
                        @Override
                        public void edit(FieldAccess f) {
                            try {
                                /* Ignore static fields / methods */
                                if ((f.getField().getModifiers() & Modifier.STATIC) == 0) {
                                    final String fieldName = f.getFieldName();
                                    final String fieldDescription = f.getSignature() + " " + f.getClassName() + "." + f.getFieldName();
                                    markKnown(fieldName, fieldDescription);


                                    if (ignoreSignatures.contains(f.getSignature())) {
                                        log.trace("Never filtering {} because signature {}", fieldName, f.getSignature());
                                        return;
                                    }
                                    if (ignoreFields.contains(fieldName)) {
                                        log.trace("Never filtering {} because fieldName", fieldName);
                                        return;
                                    }
                                    if (f.getField().getType().isPrimitive()) {
                                        log.trace("Never filtering {} because is a primitive", fieldName);
                                        return;
                                    }

                                    if (f.isReader()) {

                                        if (("Ljava/util/SortedSet;".equals(f.getSignature()) || "Ljava/util/Set;".equals(f.getSignature()))) {
                                            log.debug("Instrumenting Set {}", fieldDescription);
                                            if ("titles".equals(fieldName)) {
                                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.filter.FilteredSortedTitleSet.wrapTitles(\"" + fieldName + "\", $proceed($$));");
                                            } else if ("descriptions".equals(fieldName)) {
                                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.filter.FilteredSortedDescriptionSet.wrapDescriptions(\"" + fieldName + "\", $proceed($$));");
                                            } else {
                                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.filter.FilteredSortedSet.wrap(\"" + fieldName + "\", $proceed($$));");
                                            }
                                        } else if ("Ljava/util/List;".equals(f.getSignature())) {
                                            log.debug("Instrumenting List {}", fieldDescription);
                                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.filter.FilteredList.wrap(\"" + fieldName + "\", $proceed($$));");
                                        } else {
                                            log.debug("Instrumenting {}", fieldDescription);
                                            f.replace("$_ = $proceed($$) == null ? null : ($r)nl.vpro.api.rs.filter.FilteredObject.wrap(\"" + fieldName + "\", $proceed($$)).value();");
                                        }
                                    }
                                }
                            } catch (RuntimeException | NotFoundException | CannotCompileException exception) {
                                log.error("During instrumentation of '" + ctClass + "." + f.getFieldName() + "' : " + exception.getClass() + " " + exception.getMessage(), exception);
                            }
                        }
                    });

                    Class<?> aClass = ctClass.toClass();
                    log.info("Successfully instrumented {}", aClass);
                } catch (RuntimeException | CannotCompileException error ){
                    log.error("During instrumentation of {}: {} {}",  ctClass.getName(), error.getClass(), error.getMessage());
                    if (error.getCause() != null) {
                        log.error("Caused by {} {}", error.getCause().getClass(), error.getCause().getMessage(), error.getCause());
                    }
                }
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    private static void markKnown(String fieldName, String fieldDescription) {
          if (!knownProperties.contains(fieldName)) {
              knownProperties.add(fieldName);
          } else {
              log.trace("Found {} more than once!", fieldDescription);
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
                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.filter.ScheduleEventViewSortedSet.wrap($proceed($$));");
                            }
                        }
                    });
                    ctClass.toClass();
                } catch (RuntimeException wtf) {
                    log.warn("{}:{}", wtf.getClass().getName(), wtf.getMessage());
                }
            }
        } catch(CannotCompileException | NotFoundException e) {
            // schedule event stuff is optional
            // It is not used in the player api, so it does have ScheduleEventView.
            log.info("{}:{}", e.getClass().getName(), e.getMessage());
        }
    }

}
