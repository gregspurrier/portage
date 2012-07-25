(ns portage.core
  (:use clojure.pprint))

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

(defn- inject-arguments
  "Splices args into the form as the first argument(s)."
  [form & args]
  ;; Preserve metadata like clojure.core/-> does2
  (with-meta `(~(first form) ~@args ~@(next form)) (meta form)))

(defn- continuation-form
  "Creates a function form representing the continuation of the -+-> expression
with the remaining-forms."
  [remaining-forms]
  (let [sym (gensym)]
    `(fn [~sym] (-+-> ~sym ~@remaining-forms))))

(defmacro -+->
  "Threads x through the forms in a manner similar to the -> macro.
Continuation callbacks are automatically supplied as the first argument
to functions tagged with the :portageable metadata. This allows normal
and asynchronous functions--marked as :portageable--to be mixed in
the same flow.

If a form returns a portage error (created via portage.core/error)
any following intermediate forms will be skipped unless they are
tagged with the :accepts-errors metadata. The final form is always
invoked, regardless of error status. Use portage.core/error? to
determine whether a value is a portage error and
portage.core/error-value to retrieve a portage error's wrapped value.

-+-> always returns nil. It is expected that the final form will
do something useful with the final value."
  ([x form]
     (let [form (promote-to-list form)]
       `(do ~(inject-arguments form x) nil)))
  ([x form & more]
     (let [form (promote-to-list form)
           op (first form)]
       (if (portageable? op)
         (if (accepts-errors? op)
           `(do ~(inject-arguments form (continuation-form more) x) nil)
           `(do (if (error? ~x)
                  (-+-> ~x ~@more)
                  ~(inject-arguments form (continuation-form more) x))
                nil))
         (if (accepts-errors? op)
           `(do (-+-> (-> ~x ~form) ~@more) nil)
           `(do (if (error? ~x)
                  (-+-> ~x ~@more)
                  (-+-> (-> ~x ~form) ~@more))
                nil))))))
