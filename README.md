# Portage

You chart your course down a river of functions. Portage gets you through the asynchronous rapids.

## Example
```clojure
(use 'portage.core)

;; Portage carries your code over asynchronous bumps in the control
;; flow. To do this, the asynchonous function must be marked as
;; :portageable and accept a callback function as its first argument.
;; For example:
(defn ^:portageable async-str
  [callback & args]
  (.start (Thread. (fn []
                     (println "Awaiting asynchronous result...")
                     (Thread/sleep 3000)
                     (callback (apply str args))))))

;; Using the -+-> macro, you can thread together normal and portageable
;; asynchronous functions. The -+-> macro always returns nil: the final
;; form should do something with the result.
(-+-> "With portage "
      (str "you can mix normal ")
      (async-str "and asynchronous functions ")
      (str "in the same flow.")
      (async-str "\nCarry on.")
      println)

;; The above form will immediately return nil and then you'll see the
;; following output:
;;
;;   Awaiting asynchronous result...
;;   Awaiting asynchronous result...
;;   With portage you can mix normal and asynchronous functions in the same flow.
;;   Carry on.
```

## Handling Errors
Functions can return--or pass to the result callback, in the case of asynchronous functions--an error value created with `portage.core/error` to indicate that an error has occurred during processing. By default, all remaining intermediate forms will be skipped and only the final form will be invoked with the error value. This can be overridden by tagging functions used in intermediate forms with the :accepts-errors metadata. Any such function will receive the error value rather than being skipped.

Use `portage.core/error?` to determine whether a value is an error. The value wrapped in an error can be retrieved with `portage.core/error-value`.

## To Do
- Catch exceptions in form execution and wrap them in portage errors
- Parallel async flows
- Make `-+->>`, equivalent to `->>`
- Convenience wrapper block waiting for blocking for, and returning, final result

## License

Copyright (c) 2012 Greg Spurrier

Distributed under the MIT license. See LICENSE.txt for the details.
