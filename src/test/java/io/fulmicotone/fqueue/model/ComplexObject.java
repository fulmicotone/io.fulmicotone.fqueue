package io.fulmicotone.fqueue.model;

public class ComplexObject {


    private String name;
    private Integer age;

    private ComplexObject(Builder builder) {
        name = builder.name;
        age = builder.age;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public String getName() {
        return name;
    }

    public Integer getAge() {
        return age;
    }


    public String getJson(){
        return "{ \"name\": \""+name+"\", \"age\" : "+age+" }";
    }


    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static final class Builder {
        private String name;
        private Integer age;

        private Builder() {
        }

        public Builder withName(String val) {
            name = val;
            return this;
        }

        public Builder withAge(Integer val) {
            age = val;
            return this;
        }

        public ComplexObject build() {
            return new ComplexObject(this);
        }
    }
}
