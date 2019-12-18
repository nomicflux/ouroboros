package com.jnape.palatable.ouroboros;

import com.jnape.palatable.lambda.functions.Fn1;
import com.jnape.palatable.lambda.functions.Fn2;
import com.jnape.palatable.lambda.functions.builtin.fn2.LazyRec;
import com.jnape.palatable.lambda.functor.Functor;
import com.jnape.palatable.lambda.functor.builtin.Lazy;

import static com.jnape.palatable.lambda.functor.builtin.Lazy.lazy;

// The Lazy Catamorphism I'd want, except that it requires forcing a Lazy in order to apply the algebra,
// which causes stack overflows
public final class FixLazyCatamorphism<A, F extends Functor<?, F>, FA extends Functor<A, F>> implements
        Fn2<Algebra<FA, A>, FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>>, Lazy<A>> {

    private static final FixLazyCatamorphism<?, ?, ?> INSTANCE = new FixLazyCatamorphism<>();

    private FixLazyCatamorphism() {
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Lazy<A> checkedApply(Algebra<FA, A> algebra, FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>> fixF) {
        return LazyRec.<FixLazy<F, ? extends Functor<?, F>>, A>lazyRec((f, fixed) -> ((Lazy<? extends Lazy<? extends Functor<? extends FixLazy<F, ?>, F>>>) lazy(fixed::unfixLazy))
                .flatMap(l1 -> l1.fmap(l2 -> l2.fmap(f)))
                .flatMap(l -> lazy(() -> l.fmap(Lazy::value))) // The poisoned apple
                .<FA>fmap(Functor::coerce)
                .fmap(algebra::apply), fixF);
    }

    @SuppressWarnings("unchecked")
    public static <A, F extends Functor<?, F>, FA extends Functor<A, F>> FixLazyCatamorphism<A, F, FA> fixLazyCata() {
        return (FixLazyCatamorphism<A, F, FA>) INSTANCE;
    }

    public static <A, F extends Functor<?, F>, FA extends Functor<A, F>>
    Fn1<FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>>, Lazy<A>> fixLazyCata(Algebra<FA, A> algebra) {
        return FixLazyCatamorphism.<A, F, FA>fixLazyCata().apply(algebra);
    }

    public static <A, F extends Functor<?, F>, FA extends Functor<A, F>> Lazy<A> fixLazyCata(
            Algebra<FA, A> algebra,
            FixLazy<F, ? extends Functor<? extends FixLazy<F, ?>, F>> fixF) {
        return fixLazyCata(algebra).apply(fixF);
    }
}
