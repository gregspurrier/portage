(ns portage.core
  (:use clojure.pprint))

(defn- portageable?
  "Returns true if sym represents a portageable macro or function."
  [sym]
  (or (= sym '-+->)
      (boolean (-> sym resolve meta :portageable))))

(defn- promote-to-list
  "Promotes non-list forms to be a single element list containing that form.
Forms that are already lists are unchanged."
  [form]
  (if (seq? form)
    form
    (list form)))

(defmacro -+->
  ([f x]
     `(do (~f ~x) nil))
  ([f x form]
     (let [form (promote-to-list form)]
       (if (portageable? (first form))
         `(do (~(first form) ~f ~x ~@(next form)) nil)
         `(do (~f (~(first form) ~x ~@(next form))) nil))))
  ([f x form & more]
     (let [form (promote-to-list form)]
       (if (portageable? (first form))
         (let [sym (gensym)]
           `(do (~(first form) (fn [~sym] (-+-> ~f ~sym ~@more))
                 ~x ~@(next form))
                nil))
         `(do (-+-> ~f (-> ~x ~form) ~@more) nil)))))
