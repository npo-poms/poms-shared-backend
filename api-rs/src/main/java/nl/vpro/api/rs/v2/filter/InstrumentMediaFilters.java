/**
 * Copyright (C) 2014 All rights reserved
 * VPRO The Netherlands
 */
package nl.vpro.api.rs.v2.filter;

import javassist.*;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Roelof Jan Koekoek
 * @since 3.0
 */
public class InstrumentMediaFilters {
    private static final Logger log = LoggerFactory.getLogger(InstrumentMediaFilters.class);

    private List<String> ignoreFields = Arrays.asList("mid", "type", "avType", "sortDate", "isEmbeddable");

    public InstrumentMediaFilters() {
        instrument(
                "nl.vpro.domain.media.support.PublishableObject",
                "nl.vpro.domain.media.MediaObject",
                "nl.vpro.domain.media.Program",
                "nl.vpro.domain.media.Group",
                "nl.vpro.domain.media.Segment"
        );

        instrumentScheduleEvents("nl.vpro.domain.media.Schedule");

        ignoreFields = null; // Garbage
    }

    private void instrument(String... classNames) {
        try {
            ClassPool cp = new ClassPool(ClassPool.getDefault());
            cp.childFirstLookup = true;

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            cp.insertClassPath(new LoaderClassPath(cl));

            CtClass[] ctClasses = cp.get(classNames);

            for (CtClass ctClass : ctClasses) {
                ctClass.instrument(new ExprEditor() {
                    @Override
                    public void edit(FieldAccess f) throws CannotCompileException {
                        if (ignoreFields.contains(f.getFieldName())) {
                            // Always show
                        } else if ("Ljava/util/SortedSet;".equals(f.getSignature()) && f.isReader()) {
                            log.debug("Instrumenting SortedSet {}", f.getFieldName());
                            if (f.getFieldName().equals("titles")) {
                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v2.filter.FilteredSortedTitleSet.wrap(\"" + f.getFieldName() + "\", $proceed($$));");
                            } else {
                                f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v2.filter.FilteredSortedSet.wrap(\"" + f.getFieldName() + "\", $proceed($$));");
                            }
                        } else if ("Ljava/util/List;".equals(f.getSignature()) && f.isReader()) {
                            log.debug("Instrumenting SortedSet {}", f.getFieldName());
                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v2.filter.FilteredList.wrap(\"" + f.getFieldName() + "\", $proceed($$));");
                        } else {
                            log.debug("Instrumenting {}", f.getFieldName());
                            f.replace("$_ = $proceed($$) == null ? null : ($r)nl.vpro.api.rs.v2.filter.FilteredObject.wrap(\"" + f.getFieldName() + "\", $proceed($$)).value();");
                        }
                    }
                });

                ctClass.toClass();
            }
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void instrumentScheduleEvents(String... classNames) {
        try {
            ClassPool cp = new ClassPool(ClassPool.getDefault());
            cp.childFirstLookup = true;

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            cp.insertClassPath(new LoaderClassPath(cl));

            CtClass[] ctClasses = cp.get(classNames);

            for (CtClass ctClass : ctClasses) {
                ctClass.instrument(new ExprEditor() {
                    @Override
                    public void edit(FieldAccess f) throws CannotCompileException {
                        if ("Ljava/util/SortedSet;".equals(f.getSignature()) && f.isReader() && f.getFieldName().equals("scheduleEvents")) {
                            log.debug("Instrumenting ScheduleEvents for {}", f.getClassName());
                            f.replace("$_ = $proceed($$) == null ? null : nl.vpro.api.rs.v2.filter.ScheduleEventView.wrap($proceed($$));");
                        }

                    }
                });
                ctClass.toClass();
            }
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
