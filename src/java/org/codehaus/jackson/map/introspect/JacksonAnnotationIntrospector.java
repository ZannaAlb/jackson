package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * {@link AnnotationIntrospector} implementation that handles standard
 * Jackson annotations.
 */
public class JacksonAnnotationIntrospector
    extends AnnotationIntrospector
{
    public JacksonAnnotationIntrospector() { }

    /*
    ////////////////////////////////////////////////////
    // General annotation properties
    ////////////////////////////////////////////////////
     */

    public boolean isHandled(Annotation ann)
    {
        Class<? extends Annotation> acls = ann.annotationType();

        /* 16-May-2009, tatu: used to check this like so...
           final String JACKSON_PKG_PREFIX = "org.codehaus.jackson";

           Package pkg = acls.getPackage();
           return (pkg != null) && (pkg.getName().startsWith(JACKSON_PKG_PREFIX));
        */

        // but this is more reliable, now that we have tag annotation:
        return acls.getAnnotation(JacksonAnnotation.class) != null;
    }

    /*
    ///////////////////////////////////////////////////////
    // General class annotations
    ///////////////////////////////////////////////////////
    */

    public Boolean findCachability(AnnotatedClass ac)
    {
        JsonCachable ann = ac.getAnnotation(JsonCachable.class);
        if (ann == null) {
            return null;
        }
        return ann.value() ? Boolean.TRUE : Boolean.FALSE;
    }

    /*
    ///////////////////////////////////////////////////////
    // General method annotations
    ///////////////////////////////////////////////////////
    */

    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        JsonIgnore ann = m.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }

    /*
    ////////////////////////////////////////////////////
    // General field annotations
    ////////////////////////////////////////////////////
     */

    public boolean isIgnorableField(AnnotatedField f)
    {
        JsonIgnore ann = f.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: general annotations
    ///////////////////////////////////////////////////////
    */


    @SuppressWarnings("unchecked")
    public Class<? extends JsonSerializer<?>> findSerializerClass(Annotated a)
    {
        /* 21-May-2009, tatu: Slight change; primary annotation is now
         *    @JsonSerialize; @JsonUseSerializer is deprecated
         */
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<? extends JsonSerializer<?>> serClass = ann.using();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        JsonUseSerializer oldAnn = a.getAnnotation(JsonUseSerializer.class);
        if (oldAnn == null) {
            return null;
        }
        Class<?> serClass = oldAnn.value();
        /* 21-Feb-2009, tatu: There is now a way to indicate "no class"
         *   (to essentially denote a 'dummy' annotation, needed for
         *   overriding in some cases), need to check:
         */
        if (serClass == NoClass.class || serClass == JsonSerializer.None.class) {
            return null;
        }
        if (!JsonSerializer.class.isAssignableFrom(serClass)) {
            throw new IllegalArgumentException("Invalid @JsonUseSerializer annotation: Class "+serClass.getName()+" not a JsonSerializer");
        }
        return (Class<JsonSerializer<?>>)serClass;
    }

    public Class<?> findSerializationType(Annotated am)
    {
        // Primary annotation, JsonSerialize
        JsonSerialize ann = am.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    public boolean willWriteNullProperties(Annotated a, boolean defValue)
    {
        JsonWriteNullProperties ann = a.getAnnotation(JsonWriteNullProperties.class);
        return (ann == null) ? defValue : ann.value();
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */

    public Boolean findGetterAutoDetection(AnnotatedClass ac)
    {
        JsonAutoDetect cann = ac.getAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                for (JsonMethod jm : methods) {
                    if (jm.getterEnabled()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: method annotations
    ///////////////////////////////////////////////////////
    */

    public String findGettablePropertyName(AnnotatedMethod am)
    {
        /* 22-May-2009, tatu: JsonProperty is the primary annotation
         *   to check for
         */
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        /* 22-May-2009, tatu: JsonGetter is deprecated as of 1.1
         *    but still supported
         */
        JsonGetter ann = am.getAnnotation(JsonGetter.class);
        if (ann != null) {
            return ann.value();
        }
        /* 22-May-2009, tatu: And finally, JsonSerialize implies
         *   that there is a property, although doesn't define name
         */
        if (am.hasAnnotation(JsonSerialize.class)) {
            return "";
        }
        return null;
    }

    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        JsonValue ann = am.getAnnotation(JsonValue.class);
        // value of 'false' means disabled...
        return (ann != null && ann.value());
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: general annotations
    ///////////////////////////////////////////////////////
    */

    @SuppressWarnings("unchecked")
	public Class<? extends JsonDeserializer<?>> findDeserializerClass(Annotated a)
    {
        /* 21-May-2009, tatu: Slight change; primary annotation is now
         *    @JsonDeserialize; @JsonUseDeserializer is deprecated
         */
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends JsonDeserializer<?>> deserClass = ann.using();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        JsonUseDeserializer oldAnn = a.getAnnotation(JsonUseDeserializer.class);
        if (oldAnn == null) {
            return null;
        }
        Class<?> deserClass = oldAnn.value();
        if (deserClass == NoClass.class || deserClass == JsonDeserializer.None.class) {
            return null;
        }
        if (!JsonDeserializer.class.isAssignableFrom(deserClass)) {
            throw new IllegalArgumentException("Invalid @JsonUseDeserializer annotation: Class "+deserClass.getName()+" not a JsonDeserializer");
        }
        return (Class<JsonDeserializer<?>>)deserClass;
    }

    public Class<?> findDeserializationType(Annotated am)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        JsonClass oldAnn = am.getAnnotation(JsonClass.class);
        if (oldAnn != null) {
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    public Class<?> findDeserializationKeyType(Annotated am)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.keyAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        JsonKeyClass oldAnn = am.getAnnotation(JsonKeyClass.class);
        if (oldAnn != null) {
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    public Class<?> findDeserializationContentType(Annotated am)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.contentAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        JsonContentClass oldAnn = am.getAnnotation(JsonContentClass.class);
        if (oldAnn != null) {
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // Class annotations: Deserialization
    ////////////////////////////////////////////////////
     */

    public Boolean findSetterAutoDetection(AnnotatedClass ac)
    {
        JsonAutoDetect cann = ac.getAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                for (JsonMethod jm : methods) {
                    if (jm.setterEnabled()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }
        return null;
    }

    public Boolean findCreatorAutoDetection(AnnotatedClass ac)
    {
        JsonAutoDetect cann = ac.getAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                for (JsonMethod jm : methods) {
                    if (jm.creatorEnabled()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Method annotations: deserialization
    ///////////////////////////////////////////////////////
    */

    public String findSettablePropertyName(AnnotatedMethod am)
    {
        /* 22-May-2009, tatu: JsonProperty is the primary annotation
         *   to check for
         */
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        /* 22-May-2009, tatu: JsonSetter is deprecated as of 1.1
         *    but still supported
         */
        JsonSetter ann = am.getAnnotation(JsonSetter.class);
        if (ann != null) {
            return ann.value();
        }
        /* 22-May-2009, tatu: And finally, JsonSerialize implies
         *   that there is a property, although doesn't define name
         */
        if (am.hasAnnotation(JsonDeserialize.class)) {
            return "";
        }
        return null;
    }

    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonAnySetter.class);
    }

    public boolean hasCreatorAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonCreator.class);
    }
}
