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
        validate(null, null, null)
    }

    boolean validate(Map<String, Object> params) {
        validate(null, params, null)
    }

    boolean validate(Closure adHocConstraintsClosure) {
        validate(null, null, adHocConstraintsClosure)
    }

    boolean validate(Map<String, Object> params, Closure adHocConstraintsClosure) {
        validate(null, params, adHocConstraintsClosure)
    }

    @Override
    boolean validate(List fieldsToValidate) {
        validate(fieldsToValidate, null, null)
    }

    boolean validate(List fieldsToValidate, Map<String, Object> params) {
        validate(fieldsToValidate, params, null)
    }

    boolean validate(List fieldsToValidate, Closure adHocConstraintsClosure) {
        validate(fieldsToValidate, null, adHocConstraintsClosure)
    }

    boolean validate(List fieldsToValidate, Map<String, Object> params, Closure adHocConstraintsClosure) {
        beforeValidateHelper.invokeBeforeValidate(this, fieldsToValidate)

        boolean shouldInherit = Boolean.valueOf(params?.inherit?.toString() ?: 'true')
        Map<String, ConstrainedProperty> constraints = resolveEffectiveConstraintsMap(adHocConstraintsClosure, shouldInherit)

        def localErrors = doValidate(constraints, fieldsToValidate)

        boolean clearErrors = Boolean.valueOf(params?.clearErrors?.toString() ?: 'true')
        if (errors && !clearErrors) {
            errors.addAllErrors(localErrors)
        } else {
            errors = localErrors
        }
        return !errors.hasErrors()
    }

    private Map<String, ConstrainedProperty> resolveEffectiveConstraintsMap(Closure adHocConstraintsClosure, boolean shouldInherit) {
        Map<String, ConstrainedProperty> constraints = getConstraintsMap()
        if (!adHocConstraintsClosure) {
            return constraints
        }

        def adHocConstraints = getAddhocConstraintsMap(adHocConstraintsClosure)
        if (!shouldInherit) {
            return adHocConstraints
        }

        // Merge ad-hoc constraints and pre-declared constraints.
        // If a same constraint is given for a same property, the default is ignored.
        // Not to modify a cached map, the target to modify must be the ad-hoc map.
        for (ConstrainedProperty constrainedProperty : constraints.values()) {
            def propertyName = constrainedProperty.propertyName
            if (adHocConstraints.containsKey(propertyName)) {
                for (Constraint appliedConstraint : constrainedProperty.appliedConstraints) {
                    if (!adHocConstraints[propertyName].hasAppliedConstraint(appliedConstraint.name)) {
                        adHocConstraints[propertyName].applyConstraint(appliedConstraint.name, appliedConstraint.parameter)
                    }
                }
            } else {
                adHocConstraints[propertyName] = constrainedProperty
            }
        }
        return adHocConstraints
    }

    private ValidationErrors doValidate(Map<String, ConstrainedProperty> constraints, List fieldsToValidate) {
        def localErrors = new ValidationErrors(this, this.class.name)
        if (constraints) {
            Object messageSource = findMessageSource()
            def originalErrors = getErrors()
            for (originalError in originalErrors.allErrors) {
                if (originalError instanceof FieldError) {
                    if (originalErrors.getFieldError(originalError.field)?.bindingFailure) {
                        localErrors.addError originalError
                    }
                } else {
                    localErrors.addError originalError
                }
            }
            for (prop in constraints.values()) {
                if (fieldsToValidate == null || fieldsToValidate.contains(prop.propertyName)) {
                    def fieldError = originalErrors.getFieldError(prop.propertyName)
                    if (fieldError == null || !fieldError.bindingFailure) {
                        prop.messageSource = messageSource

                        def value = getPropertyValue(prop)
                        prop.validate(this, value, localErrors)
                    }
                }
            }
        }
        localErrors
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
