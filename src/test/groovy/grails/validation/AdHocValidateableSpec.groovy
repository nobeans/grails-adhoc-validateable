package grails.transaction

import grails.validation.AdHocValidateable
import spock.lang.Specification

class AdHocValidateableSpec extends Specification {

    void 'Test that pre-declared constraints can be used'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate()

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        nameValue | ageValue | expectedValid | nameErrorCode | ageErrorCode
        'Kirk'    | 32       | true          | null          | null
        ''        | 32       | false         | 'blank'       | null
        'Kirk'    | -1       | false         | null          | 'min.notmet'
        ''        | -1       | false         | 'blank'       | 'min.notmet'
    }

    void 'Test that ad-hoc constraints can be used'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate {
            name maxSize: 10
            age max: 18
        }

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        nameValue   | ageValue | expectedValid | nameErrorCode      | ageErrorCode
        'Kirk'      | 15       | true          | null               | null
        'Kirk' * 10 | 15       | false         | 'maxSize.exceeded' | null
        'Kirk'      | 32       | false         | null               | 'max.exceeded'
        'Kirk' * 10 | 32       | false         | 'maxSize.exceeded' | 'max.exceeded'
    }

    void 'Test that "fieldsToValidate" can be used with ad-hoc constraints'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate(['age']) {
            name maxSize: 10
            age max: 18
        }

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        nameValue   | ageValue | expectedValid | nameErrorCode | ageErrorCode
        'Kirk'      | 15       | true          | null          | null
        'Kirk' * 10 | 15       | true          | null          | null
        'Kirk'      | 32       | false         | null          | 'max.exceeded'
        'Kirk' * 10 | 32       | false         | null          | 'max.exceeded'
    }

    void 'Test that both pre-declared and ad-hoc constraints can be used together'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate {
            name maxSize: 10
        }

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        nameValue   | ageValue | expectedValid | nameErrorCode      | ageErrorCode
        'Kirk'      | 32       | true          | null               | null
        'Kirk' * 10 | 32       | false         | 'maxSize.exceeded' | null
        'Kirk'      | -1       | false         | null               | 'min.notmet'
        'Kirk' * 10 | -1       | false         | 'maxSize.exceeded' | 'min.notmet'
    }

    void 'Test that pre-declared closure can be used as ad-hoc constraints'() {
        given:
        def person = new Person(name: 'Kirk' * 10, age: 32, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean valid = person.validate(Person.adHocConstraints)

        then:
        !valid
        person.errors['name']?.code == 'maxSize.exceeded'
        person.errors['age']?.code == 'max.exceeded'
        person.errors.errorCount == 2
    }

    void 'Test that ad-hoc constraints overwrites if the same kind of constraint is given as ad-hoc'() {
        given:
        def person = new Person(name: '', age: 32, remarks: 'JUST_FOR_COVERAGE')

        expect:
        !person.validate()

        and:
        person.validate {
            name blank: true
        }

        and: 'cached pre-declared constraints should not be affected'
        !person.validate()
    }

    void 'Test that empty closure as ad-hoc constraints is equivalent with only pre-declared constraints'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate {}

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        nameValue | ageValue | expectedValid | nameErrorCode | ageErrorCode
        'Kirk'    | 32       | true          | null          | null
        ''        | 32       | false         | 'blank'       | null
        'Kirk'    | -1       | false         | null          | 'min.notmet'
        ''        | -1       | false         | 'blank'       | 'min.notmet'
    }

    void 'Test that "beforeValidator" is called with ad-hoc constraints'() {
        given:
        def person = new Person(name: 'Kirk', age: 32, remarks: 'JUST_FOR_COVERAGE')

        expect:
        person.validate {
            name maxSize: 10
            age max: 60
        }

        and:
        person.name == 'KIRK'
    }

    void 'Test that pre-declared is ignored when "inherit:false" is specified'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate(params, adHocConstraints)

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        params           | adHocConstraints     | nameValue   | ageValue | expectedValid | nameErrorCode      | ageErrorCode
        [inherit: true]  | { name maxSize: 10 } | 'Kirk'      | 32       | true          | null               | null
        [inherit: true]  | { name maxSize: 10 } | 'Kirk' * 10 | 32       | false         | 'maxSize.exceeded' | null
        [inherit: true]  | { name maxSize: 10 } | 'Kirk'      | -1       | false         | null               | 'min.notmet'
        [inherit: true]  | { name maxSize: 10 } | 'Kirk' * 10 | -1       | false         | 'maxSize.exceeded' | 'min.notmet'
        [inherit: true]  | {}                   | null        | null     | false         | 'nullable'         | 'nullable'
        [inherit: false] | { name maxSize: 10 } | 'Kirk'      | 32       | true          | null               | null
        [inherit: false] | { name maxSize: 10 } | 'Kirk' * 10 | 32       | false         | 'maxSize.exceeded' | null
        [inherit: false] | { name maxSize: 10 } | 'Kirk'      | -1       | true          | null               | null
        [inherit: false] | { name maxSize: 10 } | 'Kirk' * 10 | -1       | false         | 'maxSize.exceeded' | null
        [inherit: false] | {}                   | null        | null     | true          | null               | null  // default 'nullable:true' doesn't apply to ad-hoc constraints
    }

    void 'Test that errors are not cleared for each call when "clearErrors:false" is specified'() {
        given:
        def person = new Person(name: '', age: -1, remarks: 'JUST_FOR_COVERAGE')

        when:
        boolean actualValid = person.validate()

        then:
        !actualValid
        person.errors['name']?.code == 'blank'
        person.errors['age']?.code == 'min.notmet'

        when:
        actualValid = person.validate(params, adHocConstraints)

        then:
        actualValid == expectedValid
        person.errors['name']?.code == nameErrorCode
        person.errors['age']?.code == ageErrorCode

        where:
        params               | adHocConstraints                  | expectedValid | nameErrorCode | ageErrorCode
        [clearErrors: true]  | { age min: -9 }                   | false         | 'blank'       | null
        [clearErrors: true]  | { name blank: true }              | false         | null          | 'min.notmet'
        [clearErrors: true]  | { name blank: true; age min: -9 } | true          | null          | null
        [clearErrors: false] | { age max: -9 }                   | false         | 'blank'       | 'min.notmet'
        [clearErrors: false] | { name blank: true }              | false         | 'blank'       | 'min.notmet'
        [clearErrors: false] | { name blank: true; age min: -9 } | false         | 'blank'       | 'min.notmet'
    }

    void 'Test for a variety of overload methods'() {
        given:
        def person = new Person(name: 'Kirk', age: 32, remarks: 'JUST_FOR_COVERAGE')

        expect:
        person.validate()
        person.validate(clearErrors: true)
        !person.validate { age max: 18 }
        !person.validate(clearErrors: true, inherit: false) { age max: 18 }
        person.validate(['age'])
        person.validate(['age'], [clearErrors: true])
        !person.validate(['age']) { age max: 18 }
        !person.validate(['age'], [clearErrors: true, inherit: false]) { age max: 18 }
    }
}

class Person implements AdHocValidateable {
    String name
    Integer age
    String remarks

    static constraints = {
        name blank: false
        age min: 0
        remarks matches: /JUST_FOR_COVERAGE/ // This is for coverage
    }

    static adHocConstraints = {
        name maxSize: 10
        age max: 18
    }

    def beforeValidate() {
        name = name?.toUpperCase()
    }
}
