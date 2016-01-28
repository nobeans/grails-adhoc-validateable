package grails.validation

import grails.util.Holders
import org.grails.datastore.gorm.support.BeforeValidateHelper
import org.grails.validation.ConstrainedPropertyBuilder
import org.springframework.context.ApplicationContext
import org.springframework.context.MessageSource
import org.springframework.validation.FieldError

trait AdHocValidateable extends Validateable {

    private BeforeValidateHelper beforeValidateHelper = new BeforeValidateHelper()

    private static Map<String, ConstrainedProperty> getAddhocConstraintsMap(Closure constraints) {
        ConstrainedPropertyBuilder delegate = new ConstrainedPropertyBuilder(this)
        Closure<?> c = (Closure<?>) constraints.clone()
        c.setResolveStrategy(Closure.DELEGATE_ONLY)
        c.setDelegate(delegate)
        c.call()
        delegate.getConstrainedProperties()
    }

    @Override
    boolean validate() {
        validate(null, null)
    }

    @Override
    boolean validate(List fieldsToValidate) {
        validate(fieldsToValidate, null)
    }

    boolean validate(Closure adHocConstraintsClosure) {
        validate(null, adHocConstraintsClosure)
    }

    boolean validate(List fieldsToValidate, Closure adHocConstraintsClosure) {
        beforeValidateHelper.invokeBeforeValidate(this, fieldsToValidate)

        Map<String, ConstrainedProperty> baseConstraints = getConstraintsMap().clone() // to avoid modifying a cache object in Validateable

        if (adHocConstraintsClosure) {
            // merge ad-hoc constraints.
            // if a same constraint is given for a same property, the ad-hoc overwrites.
            def adHocConstraints = getAddhocConstraintsMap(adHocConstraintsClosure)
            for (ConstrainedProperty adhocConstrainedProperty : adHocConstraints.values()) {
                def propertyName = adhocConstrainedProperty.propertyName
                if (baseConstraints.containsKey(propertyName)) {
                    for (Constraint appliedConstraint : adhocConstrainedProperty.appliedConstraints) {
                        baseConstraints[propertyName].applyConstraint(appliedConstraint.name, appliedConstraint.parameter)
                    }

                } else {
                    baseConstraints[propertyName] = adhocConstrainedProperty
                }
            }
        }

        def localErrors = new ValidationErrors(this, this.class.name)
        doValidate(localErrors, baseConstraints, fieldsToValidate)
        errors = localErrors
        return !errors.hasErrors()
    }

    private ValidationErrors doValidate(ValidationErrors resultErrors, Map<String, ConstrainedProperty> constraints, List fieldsToValidate) {
        if (constraints) {
            Object messageSource = findMessageSource()
            def originalErrors = getErrors()
            for (originalError in originalErrors.allErrors) {
                if (originalError instanceof FieldError) {
                    if (originalErrors.getFieldError(originalError.field)?.bindingFailure) {
                        resultErrors.addError originalError
                    }
                } else {
                    resultErrors.addError originalError
                }
            }
            for (prop in constraints.values()) {
                if (fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    def fieldError = originalErrors.getFieldError(prop.propertyName)
                    if (fieldError == null || !fieldError.bindingFailure) {
                        prop.messageSource = messageSource

                        def value = getPropertyValue(prop)
                        prop.validate(this, value, resultErrors)
                    }
                }
            }
        }
        resultErrors
    }

    private Object getPropertyValue(ConstrainedProperty prop) {
        this.getProperty(prop.propertyName)
    }

    private MessageSource findMessageSource() {
        try {
            ApplicationContext ctx = Holders.applicationContext
            MessageSource messageSource = ctx?.containsBean('messageSource') ? ctx.getBean('messageSource', MessageSource) : null
            return messageSource
        } catch (Throwable e) {
            return null
        }
    }
}
