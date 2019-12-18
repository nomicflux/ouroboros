package com.jnape.palatable.ouroboros;

import com.jnape.palatable.lambda.adt.Maybe;
import com.jnape.palatable.lambda.adt.hlist.Tuple2;
import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.builtin.fn2.Cons;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;
import com.jnape.palatable.lambda.monad.transformer.builtin.MaybeT;
import org.junit.Test;

import java.util.ArrayList;

import static com.jnape.palatable.lambda.adt.Maybe.just;
import static com.jnape.palatable.lambda.adt.Maybe.nothing;
import static com.jnape.palatable.lambda.adt.hlist.HList.tuple;
import static com.jnape.palatable.lambda.functions.builtin.fn1.Constantly.constantly;
import static com.jnape.palatable.lambda.functions.builtin.fn2.ToCollection.toCollection;
import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;
import static com.jnape.palatable.lambda.monad.transformer.builtin.MaybeT.maybeT;
import static com.jnape.palatable.ouroboros.Anamorphism.ana;
import static com.jnape.palatable.ouroboros.Catamorphism.cata;
import static com.jnape.palatable.ouroboros.Fix.fix;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

public class AnamorphismTest {

    @Test
    public void fromMaybe() {
        Coalgebra<Integer, Maybe<Integer>> generateToSize = x -> x < 3 ? just(x + 1) : nothing();
        Functor<? extends Fix<Maybe<?>, ?>, Maybe<?>> unfix = ana(generateToSize, 0).unfix();

        assertEquals(unfix, just(fix(just(fix(just(fix(nothing())))))));
    }

    @Test
    public void collatzFromTuple() {
        Coalgebra<Integer, MaybeT<Tuple2<Integer, ?>, Integer>> coalgebra = x -> x == 1 ? maybeT(tuple(x, nothing()))
                : x % 2 == 0 ? maybeT(tuple(x, just(x / 2))) : maybeT(tuple(x, just(3 * x + 1)));

        assertEquals(ana(coalgebra, 4).unfix(),
                maybeT(tuple(4, just(fix(maybeT(tuple(2, just(fix(maybeT(tuple(1, nothing())))))))))));
    }

    @Test
    public void collatzToCata() {
        Coalgebra<Long, MaybeT<Tuple2<Long, ?>, Long>> coalgebra = x -> x == 1 ? maybeT(tuple(x, nothing()))
                : x % 2 == 0 ? maybeT(tuple(x, just(x / 2))) : maybeT(tuple(x, just(3 * x + 1)));

        Algebra<MaybeT<Tuple2<Long, ?>, Lazy<Iterable<Long>>>, Lazy<Iterable<Long>>> algebra = mtii -> {
            Tuple2<Long, Maybe<Lazy<Iterable<Long>>>> run = mtii.run();
            return lazy(Cons.cons(run._1(), run._2().match(constantly(emptyList()),
                    Lazy::value)));
        };

        Fn1<Long, Lazy<Iterable<Long>>> hylo = ana(coalgebra).fmap(cata(algebra));
        assertEquals(toCollection(ArrayList::new, hylo.apply(3L).value()), asList(3L, 10L, 5L, 16L, 8L, 4L, 2L, 1L));
    }

}