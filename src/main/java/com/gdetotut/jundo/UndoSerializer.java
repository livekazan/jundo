package com.gdetotut.jundo;

import java.io.*;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * The UndoSerializer class is responsible for correct serialization and deserialization of entire {@link UndoStack}.
 * <p>Stack encodes to Base64 using the <a href="#url">URL and Filename safe</a> type base64 encoding scheme.
 * <p>UndoSerializer has a number of useful properties to restore stack correctly:
 * <ul>
 *     <li>id allows to save an unique identifier of stack's subject</li>
 *     <li>version can be very useful when saved version and new version of object are not equal so migration needed.</li>
 *     <li>The map "extras" allows to save other extra parameters in the 'key-value' form</li>
 * </ul>
 */
public class UndoSerializer implements Serializable {

    // TODO: 04.01.18 Требует перевода!
    /**
     *   В случае объекта который не имеет маркера {@link Serializable}.
     */
    public interface OnSerializeSubj {
        /**
         * @param subj Required (should not be null).
         * @return
         */
        String toStr(Object subj);
    }

    /**
     *
     */
    public interface OnDeserializeSubj {
        /**
         *
         * @param subjAsString Required (should not be null).
         * @param subjInfo Required (should not be null).
         * @return
         */
        Object toSubj(String subjAsString, SubjInfo subjInfo);
    }

    public static class SubjInfo implements Serializable{
        public final String id;
        public final int version;
        public final Class clazz;
        public final Map<String, Serializable> extras = new TreeMap<>();

        public SubjInfo(String id, int version, Class clazz) {
            this.id = id;
            this.version = version;
            this.clazz = clazz;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            SubjInfo subjInfo = (SubjInfo) o;
            return version == subjInfo.version &&
                    Objects.equals(id, subjInfo.id) &&
                    Objects.equals(clazz, subjInfo.clazz) &&
                    Objects.equals(extras, subjInfo.extras);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, version, clazz, extras);
        }
    }

    private static class InnerStruct implements Serializable{
        UndoStack stack;
        Serializable subj;
        SubjInfo subjInfo;
    }

    private transient UndoStack stack;

    public final SubjInfo subjInfo;

    private boolean expected = true;

    /**
     * Serializes object to Base64 string.
     * @param obj object to serialize. Required (should not be null).
     * @param doZip flag for gzipping.
     * @param onSerializeSubj delegate for manual converting non-serializable {@link UndoStack#subj} to String. Can bu null.
     * @return Object as base64 string.
     * @throws IOException when something goes wrong.
     */
    public static String serialize(UndoSerializer obj, boolean doZip,
                                   OnSerializeSubj onSerializeSubj) throws IOException {
        if (obj == null) {
            throw new NullPointerException("obj");
        } else {
            InnerStruct innerStruct = new InnerStruct();
            innerStruct.stack = obj.stack;
            Object subj = innerStruct.stack.getSubj();
            if (!(subj instanceof Serializable)) {
                if (onSerializeSubj == null) {
                    throw new IOException("need Serializable");
                } else {
                    innerStruct.subj = onSerializeSubj.toStr(subj);
                }
            } else {
                innerStruct.subj = (Serializable) subj;
            }
            innerStruct.subjInfo = obj.subjInfo;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
                oos.writeObject(innerStruct);
            }

            if (doZip) {
                final ByteArrayOutputStream zippedBaos = new ByteArrayOutputStream();
                try (GZIPOutputStream gzip = new GZIPOutputStream(zippedBaos)) {
                    gzip.write(baos.toByteArray());
                }
                baos = zippedBaos;
            }
            return Base64.getUrlEncoder().encodeToString(baos.toByteArray());
        }
    }

    /**
     * Deserializes base64 string back to object.
     * @param base64 base64 string.
     * @param onDeserializeSubj delegate for manual restoring non-serializable {@link UndoStack#subj}
     *                          from String. Can bu null.
     * @return Object.
     * @throws IOException when something goes wrong.
     * @throws ClassNotFoundException when something goes wrong.
     */
    public static UndoSerializer deserialize(String base64, OnDeserializeSubj onDeserializeSubj)
            throws IOException, ClassNotFoundException {
        if (base64 == null) {
            throw new NullPointerException("base64");
        } else {

            final byte[] data = Base64.getUrlDecoder().decode(base64);
            final boolean zipped = (data[0] == (byte) (GZIPInputStream.GZIP_MAGIC))
                    && (data[1] == (byte) (GZIPInputStream.GZIP_MAGIC >> 8));

            try (ObjectInputStream ois = zipped
                    ? new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(data)))
                    : new ObjectInputStream(new ByteArrayInputStream(data))) {

                boolean isExp = true;
                InnerStruct struct = (InnerStruct) ois.readObject();
                SubjInfo subjInfo = struct.subjInfo;
                UndoStack stack = struct.stack;
                if (struct.subj.getClass() == subjInfo.clazz) {
                    stack.setSubj(struct.subj);
                } else if (struct.subj instanceof String && onDeserializeSubj != null) {
                    Object subj = onDeserializeSubj.toSubj((String) struct.subj, subjInfo);
                    if (subj == null) {
                        isExp = false;
                        subj = new Object();
                    }
                    stack.setSubj(subj);
                }
                UndoSerializer obj = new UndoSerializer(subjInfo.id, subjInfo.version, stack);
                obj.expected = isExp;
                return obj;
            }
        }
    }

    /**
     * Makes object with specific parameters.
     * @param id unique identifier allowing recognize subject on the deserialization side.
     *           Can be fully qualified class name for example, or some GUID, etc. Can be null withal.
     * @param version version of subject for correct restoring in the possible case of object migration.
     * @param stack stack itself. Required.
     */
    public UndoSerializer(String id, int version, UndoStack stack) {
        if (stack == null) {
            throw new NullPointerException("stack");
        } else {
            this.subjInfo = new UndoSerializer.SubjInfo(id, version, stack.getSubj().getClass());
            this.stack = stack;
        }
    }

    /**
     * @return Saved stack.
     */
    public UndoStack getStack() {
        return stack;
    }

    // TODO: 08.01.18 Перевод!
    /**
     * It is necessary to make sure that restored subject has expected type.
     * <p>For this sake
     * @return Flag whether the restored subject has the type as as expected.
     */
    public boolean asExpected() {
        return expected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UndoSerializer that = (UndoSerializer) o;
        return Objects.equals(stack, that.stack) &&
                Objects.equals(subjInfo, that.subjInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stack, subjInfo);
    }
}
