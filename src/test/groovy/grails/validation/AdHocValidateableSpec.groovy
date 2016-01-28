package grails.transaction

import grails.validation.AdHocValidateable
import spock.lang.Specification

class AdHocValidateableSpec extends Specification {

    class Person implements AdHocValidateable {
        String name
        Integer age

        static constraints = {
            name blank: false
            age min: 0
        }

        static adHocConstraints = {
            name maxSize: 10
            age max: 18
        }

        def beforeValidate() {
            name *= 2
        }
    }

    void 'regular validate: right properties'() {
        given:
        def person = new Person(name: 'Bart', age: 21)

        expect:
        person.validate()
    }

    void 'regular validate: wrong properties'() {
        given:
        def person = new Person(name: '', age: -1)

        when:
        boolean valid = person.validate()

        then:
        !valid
        person.errors['name']?.code == 'blank'
        person.errors['age']?.code == 'min.notmet'
        person.errors.errorCount == 2
    }

    void 'ad-hoc validate: right properties'() {
        given:
        def person = new Person(name: 'Bart', age: 21)

        expect:
        person.validate {
            name maxSize: 10
            age max: 60
        }
    }

    void 'ad-hoc validate: wrong properties for ad-hoc constraints'() {
        given:
        def person = new Person(name: 'Bart' * 10, age: 21)

        when:
        boolean valid = person.validate {
            name maxSize: 10
            age max: 18
        }

        then:
        !valid
        person.errors['name']?.code == 'maxSize.exceeded'
        person.errors['age']?.code == 'max.exceeded'
        person.errors.errorCount == 2
    }

    void 'ad-hoc validate: a wrong properties for default constraint and ad-hoc constraint respectively'() {
        given:
        def person = new Person(name: 'Bart' * 10, age: -1)

        when:
        boolean valid = person.validate {
            name maxSize: 10
        }

        then:
        !valid
        person.errors['name']?.code == 'maxSize.exceeded'
        person.errors['age']?.code == 'min.notmet'
        person.errors.errorCount == 2
    }

    void 'ad-hoc validate: pre-declared closure can be used'() {
        given:
        def person = new Person(name: 'Bart' * 10, age: 21)

        when:
        boolean valid = person.validate(Person.adHocConstraints)

        then:
        !valid
        person.errors['name']?.code == 'maxSize.exceeded'
        person.errors['age']?.code == 'max.exceeded'
        person.errors.errorCount == 2
    }

    void 'ad-hoc validate: if a same constraint is given as ad-hoc, the ad-hoc overwrites the default constraint'() {
        given:
        def person = new Person(name: '', age: 5)

        expect:
        person.validate {
            name blank: true
        }
    }

    void 'beforeValidator is available with ad-hoc validate'() {
        given:
        def person = new Person(name: 'Bart', age: 21)

        expect:
        person.validate {
            name maxSize: 10
            age max: 60
        }

        and:
        person.name == 'BartBart'
    }
}
