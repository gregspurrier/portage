(ns portage.core)

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

;; clojure.core/-> is careful to preserve the metadata of the forms that
;; it modifies, so we mirror that practice here.
(defn- inject-first-arg
  [form arg]
  (with-meta `(~(first form) ~arg ~@(next form)) (meta form)))

(defn- inject-k-and-first-arg
  [form k arg]
  (with-meta `(~(first form) ~k ~arg ~@(next form)) (meta form)))

(defn- inject-last-arg
  [form arg]
  (with-meta `(~@form ~arg) (meta form)))

(defn- inject-k-and-last-arg
  [form k arg]
  (with-meta `(~(first form) ~k ~@(next form) ~arg) (meta form)))

(defn- continuation-form
  "Creates a function form representing the continuation of the
portage threading expression with the remaining-forms."
  [threader remaining-forms]
  (let [sym (gensym)]
    `(fn [~sym] (~threader ~sym ~@remaining-forms))))

(defn- build-terminal-step
  [argument-injector x form]
  (let [form (promote-to-list form)]
    `(do ~(argument-injector form x) nil)))

(defn- build-non-terminal-step
  [thread-sym p-thread-sym argument-injector x form more]
  (let [form (promote-to-list form)
        rator (first form)]
    (if (portageable? rator)
      (if (accepts-errors? rator)
        `(do ~(argument-injector form (continuation-form p-thread-sym more) x)
             nil)
        (let [val-sym (gensym)]
          `(let [~val-sym ~x]
             (if (error? ~val-sym)
               (~p-thread-sym ~val-sym ~@more)
               ~(argument-injector form
                                   (continuation-form p-thread-sym more)
                                   val-sym))
             nil)))
      (if (accepts-errors? rator)
        `(do (~p-thread-sym (-> ~x ~form) ~@more) nil)
        (let [val-sym (gensym)]
          `(let [~val-sym ~x]
             (if (error? ~val-sym)
               (~p-thread-sym ~val-sym ~@more)
               (~p-thread-sym (~thread-sym ~val-sym ~form) ~@more))
             nil))))))

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
     (build-terminal-step inject-first-arg x form))
  ([x form & more]
     (build-non-terminal-step '-> '-+-> inject-k-and-first-arg x form more)))

(defmacro -+->>
  "The portage equivalent of the ->> macro. Behaves like -+->, except
that the threaded result is inserted at the end of the form rather
than as the first argument."
  ([x form]
     (build-terminal-step inject-last-arg x form))
  ([x form & more]
     (build-non-terminal-step '->> '-+->> inject-k-and-last-arg x form more)))

(defn run
  "Given a porteagable function and its non-continuation arguments,
  runs the function and then waits for and returns the result."
  [pf & args]
  (let [result-promise (promise)]
    (apply pf (fn [result] (deliver result-promise result)) args)
    @result-promise))