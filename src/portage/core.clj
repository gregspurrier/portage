(ns portage.core
  (:use clojure.pprint))

(defn- portageable?
  "Returns true if sym represents a portageable macro or function."
  [sym]
  (boolean (-> sym resolve meta :portageable)))

(defn- promote-to-list
  "Promotes non-list forms to be a single element list containing that form.
Forms that are already lists are unchanged."
  [form]
  (if (seq? form)
    form
    (list form)))

(defmacro -+->
  ([x form]
     (let [form (promote-to-list form)]
       `(do (~(first form) ~x ~@(next form))
            nil)))
  ([x form & more]
     (let [form (promote-to-list form)]
       (if (portageable? (first form))
         (let [sym (gensym)]
           `(do (~(first form) (fn [~sym] (-+-> ~sym ~@more)) ~x ~@(next form))
                nil))
         `(do (-+-> (-> ~x ~form) ~@more)
              nil)))))
