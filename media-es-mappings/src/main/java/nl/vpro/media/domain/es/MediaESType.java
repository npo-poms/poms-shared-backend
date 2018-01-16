package nl.vpro.media.domain.es;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * @author Michiel Meeuwissen
 * @since 4.7
 */
public enum MediaESType {
    program,
    group,
    segment,
    deletedprogram,
    deletedgroup,
    deletedsegment,
    programMemberRef,
    groupMemberRef,
    segmentMemberRef,
    episodeRef
    ;


    public String mapping() {
        return "es5/mapping/" + name() + ".json";
    }
    public String source() {
        return ApiMediaIndex.source(mapping());
    }

    public static MediaESType[] MEDIAOBJECTS = {program, group, segment};

    public static MediaESType[] MEMBERREFS= {programMemberRef, groupMemberRef, segmentMemberRef};

    public static MediaESType[] REFS = {programMemberRef, groupMemberRef, segmentMemberRef, episodeRef};

    public static MediaESType[] DELETED_MEDIAOBJECTS = {deletedprogram, deletedgroup, deletedsegment};


    public static String[] toString(MediaESType... types) {
        return Arrays.stream(types).map(Enum::name).toArray(String[]::new);

    }


    public static String[] mediaObjects() {
        return toString(MEDIAOBJECTS);
    }

    public static MediaESType memberRef(Class<?> clazz) {
        return valueOf(valueOf(clazz).name() + "MemberRef");
    }

    public static MediaESType valueOf(Class<?> clazz) {
        return valueOf(clazz.getSimpleName().toLowerCase());
    }

    public static MediaESType deletedValueOf(Class<?> clazz) {
        return valueOf("deleted" + clazz.getSimpleName().toLowerCase());
    }


    public static String[] memberRefs() {
        return toString(MEMBERREFS);
    }

    public static String[] deletedMediaObjects() {
        return toString(DELETED_MEDIAOBJECTS);
    }

    public static boolean isDeleted(String type) {
        return type.startsWith("deleted");
    }

    public static String[] nonRefs() {
        return toString(
            Stream.concat(
                Arrays.stream(MEDIAOBJECTS),
                Arrays.stream(DELETED_MEDIAOBJECTS)
            ).toArray(MediaESType[]::new));
    }
    public static String[] refs() {
        return toString(REFS);
    }


}
