package com.jnape.palatable.ouroboros;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.Fn2;
import com.jnape.palatable.lambda.functions.builtin.fn2.LazyRec;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;

import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;

// The Lazy Catamorphism which doesn't stack overflow, though it both requires FixLazy (to connect with the LazyAnamorphism)
// as well as an algebra which works on a Lazy value (which is brittle to boot - any forcing in the algebra will cause
// stack overflows
public final class AllLazyCatamorphism<A, F extends Functor<?, F>, FLA extends Functor<Lazy<A>, F>> implements
        Fn2<Algebra<FLA, Lazy<A>>, FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>>, Lazy<A>> {

    private static final AllLazyCatamorphism<?, ?, ?> INSTANCE = new AllLazyCatamorphism<>();

    private AllLazyCatamorphism() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Lazy<A> checkedApply(Algebra<FLA, Lazy<A>> algebra, FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>> fixF) {
        return LazyRec.<FixLazy<F, ? extends Functor<?, F>>, A>lazyRec((f, fixed) -> ((Lazy<? extends Lazy<? extends Functor<? extends FixLazy<F, ?>, F>>>) lazy(fixed::unfixLazy))
                .flatMap(l -> l.fmap(l2 -> l2.fmap(f)))
                .<FLA>fmap(Functor::coerce)
                .flatMap(algebra::apply), fixF);
    }

    @SuppressWarnings("unchecked")
    public static <A, F extends Functor<?, F>, FA extends Functor<Lazy<A>, F>> AllLazyCatamorphism<A, F, FA> allLazyCata() {
        return (AllLazyCatamorphism<A, F, FA>) INSTANCE;
    }

    public static <A, F extends Functor<?, F>, FA extends Functor<Lazy<A>, F>>
    Fn1<FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>>, Lazy<A>> allLazyCata(Algebra<FA, Lazy<A>> algebra) {
        return AllLazyCatamorphism.<A, F, FA>allLazyCata().apply(algebra);
    }

    public static <A, F extends Functor<?, F>, FA extends Functor<Lazy<A>, F>> Lazy<A> allLazyCata(
            Algebra<FA, Lazy<A>> algebra,
            FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>> fixF) {
        return allLazyCata(algebra).apply(fixF);
    }
}
