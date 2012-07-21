(ns portage.core)

(defn- portageable?
  [sym]
  (boolean (-> sym resolve meta :portageable)))

(defmacro -+->
  ([f x]
     `(do (~f ~x) nil))
  ([f x form]
     (if (portageable? (first form))
       `(do (~(first form) ~f ~x ~@(next form)) nil)
       `(do (~f (~(first form) ~x ~@(next form))) nil)))
  ([f x form & more]
     (if (portageable? (first form))
       (let [sym (gensym)]
         `(do (~(first form) (fn [~sym] (-+-> ~f ~sym ~@more))
               ~x ~@(next form))
              nil))
       `(do (-+-> ~f (-> ~x ~form) ~@more) nil))))