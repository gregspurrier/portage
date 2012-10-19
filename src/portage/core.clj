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
  [rator tag]
  (boolean (and (symbol? rator)
                (-> rator resolve meta tag))))

(defn- ^:testable portageable?
  "Returns true if rator represents a portageable function."
  [rator]
  (tagged-with? rator :portageable))

(defn- accepts-errors?
  "Returns true if rator represents a function that accepts portage errors."
  [rator]
  (tagged-with? rator :accepts-errors))

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
  "Creates a function form representing the continuation of the
portage threading expression with the remaining-forms."
  [threader remaining-forms]
  (let [sym (gensym)]
    `(fn [~sym] (~threader ~sym ~@remaining-forms))))

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
           rator (first form)]
       (if (portageable? rator)
         (if (accepts-errors? rator)
           `(do ~(inject-arguments form (continuation-form '-+-> more) x) nil)
           (let [val-sym (gensym)]
             `(let [~val-sym ~x]
                (if (error? ~val-sym)
                  (-+-> ~val-sym ~@more)
                  ~(inject-arguments form (continuation-form '-+-> more) val-sym))
                nil)))
         (if (accepts-errors? rator)
           `(do (-+-> (-> ~x ~form) ~@more) nil)
           (let [val-sym (gensym)]
             `(let [~val-sym ~x]
                (if (error? ~val-sym)
                  (-+-> ~val-sym ~@more)
                  (-+-> (-> ~val-sym ~form) ~@more))
                nil)))))))

(defn- inject-arguments-at-end
  "Splices args into the form as the last argument(s)."
  [form & args]
  ;; Preserve metadata like clojure.core/-> does2
  (let [[arg1 arg2] args]
    (if arg2
      ;; arg1 is continuation arg2 is the real argument
      (with-meta `(~(first form) ~arg1 ~@(next form) ~arg2) (meta form))
      (with-meta `(~@form ~arg1) (meta form)))))


(defmacro -+->>
  "The portage equivalent of the ->> macro. Behaves like -+->, except
that the threaded result is inserted at the end of the form rather
than as the first argument."
  ([x form]
     (let [form (promote-to-list form)]
       `(do ~(inject-arguments-at-end form x) nil)))
  ([x form & more]
     (let [form (promote-to-list form)
           rator (first form)]
       (if (portageable? rator)
         (if (accepts-errors? rator)
           `(do ~(inject-arguments-at-end form
                                          (continuation-form '-+->> more)
                                          x)
                nil)
           (let [val-sym (gensym)]
             `(let [~val-sym ~x]
                (if (error? ~val-sym)
                  (-+->> ~val-sym ~@more)
                  ~(inject-arguments-at-end form
                                            (continuation-form `-+->> more)
                                            val-sym))
                nil)))
         (if (accepts-errors? rator)
           `(do (-+->> (-> ~x ~form) ~@more) nil)
           (let [val-sym (gensym)]
             `(let [~val-sym ~x]
                (if (error? ~val-sym)
                  (-+->> ~val-sym ~@more)
                  (-+->> (->> ~val-sym ~form) ~@more))
                nil)))))))

(defn run
  "Given a porteagable function and its non-continuation arguments,
  runs the function and then waits for and returns the result."
  [pf & args]
  (let [result-promise (promise)]
    (apply pf (fn [result] (deliver result-promise result)) args)
    @result-promise))