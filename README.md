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
      (async-str "and asyncronous functions ")
      (str "in the same flow.")
      (async-str "\nCarry on.")
      println)

;; The above form will immediately return nil and then you'll see the
;; following output:
;;
;;   Awaiting asynchronous result...
;;   Awaiting asynchronous result...
;;   With portage you can mix normal and asyncronous functions in the same flow.
;;   Carry on.

## To Do
- Make `-+->>`, equivalent to `->>`
- Monadic (?) handling and propagation of errors down the flow
- Parallel async flows
- Convenience wrapper block waiting for blocking for, and returning, final result

## License

Copyright (c) 2012 Greg Spurrier

Distributed under the MIT license. See LICENSE.txt for the details.
