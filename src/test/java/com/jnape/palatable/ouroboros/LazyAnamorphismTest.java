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
import static com.jnape.palatable.lambda.functions.builtin.fn2.ToCollection.toCollection;
import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;
import static com.jnape.palatable.lambda.monad.transformer.builtin.MaybeT.maybeT;
import static com.jnape.palatable.ouroboros.LazyAnamorphism.lazyAna;
import static com.jnape.palatable.ouroboros.FixLazyCatamorphism.fixLazyCata;
import static com.jnape.palatable.ouroboros.AllLazyCatamorphism.allLazyCata;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public class LazyAnamorphismTest {
    @Test
    public void doesntStackOverflow() {
        Coalgebra<Integer, Tuple2<Integer, Integer>> coalgebra = x -> x % 2 == 0 ? tuple(x, x / 2) : tuple(x, 3 * x + 1);

        FixLazy<Tuple2<Integer, ?>, ? extends Functor<? extends FixLazy<Tuple2<Integer, ?>, ?>, Tuple2<Integer, ?>>> ana = lazyAna(coalgebra, 15);
        ana.unfixLazy();
    }

    @Test
    public void withCata() {
        Coalgebra<Long, MaybeT<Tuple2<Long, ?>, Long>> coalgebra =
                value -> value == 1 ? maybeT(tuple(value, nothing()))
                        : value % 2 == 0 ? maybeT(tuple(value, just(value / 2))) : maybeT(tuple(value, just(3 * value + 1)));

        Algebra<MaybeT<Tuple2<Long, ?>, Iterable<Long>>, Iterable<Long>> algebra = mtii -> {
            Tuple2<Long, Maybe<Iterable<Long>>> run = mtii.run();
            return Cons.cons(run._1(), run._2().orElse(emptyList()));
        };

        Fn1<Long, Lazy<Iterable<Long>>> hylo = lazyAna(coalgebra).fmap(FixLazyCatamorphism.fixLazyCata(algebra));
        assertEquals(toCollection(ArrayList::new, hylo.apply(3L).value()), asList(3L, 10L, 5L, 16L, 8L, 4L, 2L, 1L));
    }

    @Test
    public void longListWithLazyCata() {
        Coalgebra<Long, MaybeT<Tuple2<Long, ?>, Long>> coalgebra =
                value -> value == 1 ? maybeT(tuple(value, nothing()))
                        : value % 2 == 0 ? maybeT(tuple(value, just(value / 2))) : maybeT(tuple(value, just(3 * value + 1)));

        Algebra<MaybeT<Tuple2<Long, ?>, Lazy<Iterable<Long>>>, Lazy<Iterable<Long>>> lazyAlgebra = mtii -> {
            Tuple2<Long, Maybe<Lazy<Iterable<Long>>>> run = mtii.run();
            return lazy(run::_1).flatMap(x -> run._2().fmap(l -> l.fmap(y -> Cons.cons(x, y))).orElse(lazy(singletonList(x))));
        };

        Fn1<Long, Lazy<Iterable<Long>>> lazyHylo = lazyAna(coalgebra).fmap(AllLazyCatamorphism.allLazyCata(lazyAlgebra));

        ArrayList<Long> collatz = toCollection(ArrayList::new, lazyHylo.apply(9780657630L).value());
    }
}