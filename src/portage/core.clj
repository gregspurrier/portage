(ns portage.core
  (:use clojure.pprint))

(defn- tagged-with?
  [sym tag]
  (boolean (-> sym resolve meta tag)))

(defn- portageable?
  "Returns true if sym represents a portageable function."
  [sym]
  (tagged-with? sym :portageable))

(defn- accepts-errors?
  "Returns true if sym represents a function that accepts portage errors."
  [sym]
  (tagged-with? sym :accepts-errors))

(defn- promote-to-list
  "Promotes non-list forms to be a single element list containing that form.
Forms that are already lists are unchanged."
  [form]
  (if (seq? form)
    form
    (list form)))

(defn error
  "Create a Portage error having the value x"
  [x]
  {:portage-error x})

(defn error?
  [x]
  (and (map? x)
       (contains? x :portage-error)))

(defn error-value
  [x]
  (:portage-error x))

(defmacro -+->
  ([x form]
     (let [form (promote-to-list form)]
       `(do (~(first form) ~x ~@(next form))
            nil)))
  ([x form & more]
     (let [form (promote-to-list form)]
       (if (portageable? (first form))
         (let [sym (gensym)]
           (if (accepts-errors? (first form))
             `(do (~(first form) (fn [~sym] (-+-> ~sym ~@more))
                     ~x ~@(next form)) nil)
             `(do (if (error? ~x)
                    (-+-> ~x ~@more)
                    (~(first form) (fn [~sym] (-+-> ~sym ~@more))
                     ~x ~@(next form)))
                  nil)))
         (if (accepts-errors? (first form))
           `(do (-+-> (-> ~x ~form) ~@more) nil)
           `(do (if (error? ~x)
                  (-+-> ~x ~@more)
                  (-+-> (-> ~x ~form) ~@more))
                nil))))))
