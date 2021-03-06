package it.unibz.inf.ontop.model.type.impl;


import com.google.common.collect.ImmutableList;
import it.unibz.inf.ontop.exception.IncompatibleTermException;
import it.unibz.inf.ontop.model.type.TermType;

import java.util.Optional;
import java.util.stream.IntStream;

import static it.unibz.inf.ontop.model.OntopModelSingletons.TYPE_FACTORY;
import static it.unibz.inf.ontop.model.term.functionsymbol.Predicate.COL_TYPE.*;


public class NumericTermTypeInferenceRule extends UnifierTermTypeInferenceRule {

    /**
     * Checks that all the terms are numeric
     */
    protected void doAdditionalChecks(ImmutableList<Optional<TermType>> argumentTypes)
            throws IncompatibleTermException {
        IntStream.range(0, argumentTypes.size())
                .forEach(i ->  {
                    if(!argumentTypes.get(i)
                            .map(t -> NUMERIC_TYPES.contains(t.getColType()))
                            .orElse(true)) {

                        throw new IncompatibleTermException("numeric term", argumentTypes.get(i).get());
                    }
                });
    }

    /**
     * Only base numeric types (double, float, decimal and integer) can be returned by numeric functions and operators
     */
    @Override
    protected Optional<TermType> postprocessInferredType(Optional<TermType> optionalTermType) {
        return optionalTermType
                .map(t -> INTEGER_TYPES.contains(t.getColType()) ? TYPE_FACTORY.getTermType(INTEGER) : t);
    }
}
