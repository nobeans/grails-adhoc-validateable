package grails.transaction

import grails.validation.AdHocValidateable
import spock.lang.Specification

class AdHocValidateableSpec extends Specification {

    void 'Test that pre-declared constraints can be used'() {
        given:
        def person = new Person(name: nameValue, age: ageValue, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: nameValue, age: ageValue, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: nameValue, age: ageValue, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: nameValue, age: ageValue, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: 'Kirk' * 10, age: 32, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: '', age: 32, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: nameValue, age: ageValue, remarks: 'NOT_TO_OVERWRITE')

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
        def person = new Person(name: 'Kirk', age: 32, remarks: 'NOT_TO_OVERWRITE')

        expect:
        person.validate {
            name maxSize: 10
            age max: 60
        }

        and:
        person.name == 'KIRK'
    }
}

class Person implements AdHocValidateable {
    String name
    Integer age
    String remarks

    static constraints = {
        name blank: false
        age min: 0
        remarks matches: /NOT_TO_OVERWRITE/
    }

    static adHocConstraints = {
        name maxSize: 10
        age max: 18
    }

    def beforeValidate() {
        name = name.toUpperCase()
    }
}
