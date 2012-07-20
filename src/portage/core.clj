(ns portage.core)

(defn- portageable?
  [sym]
  (boolean (-> sym resolve meta :portageable)))

(defmacro portage->
  ([x f]
     `(do (~f ~x) nil))
  ([x f form]
     (if (portageable? (first form))
       `(do (~(first form) ~f ~x ~@(next form)) nil)
       `(do (~f (~(first form) ~x ~@(next form))) nil)))
  ([x f form & more]
     (if (portageable? (first form))
       (let [sym (gensym)]
         `(do (~(first form) (fn [~sym] (portage-> ~sym ~f ~@more))
               ~x ~@(next form))
              nil))
       `(do (portage-> (-> ~x ~form) ~f ~@more) nil))))