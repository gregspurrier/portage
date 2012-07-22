# Portage

You chart your course down a river of functions. Portage gets you through the asynchronous rapids.

## Example
```clojure
(use 'portage.core)

;; A synchronous function that doubles a number
(defn times-two [x]
  (println "doubling...")
  (* x 2))

;; An asynchronous version that hands off control to an agent and
;; takes a while to compute the result and invoke the callback.
;; Note that async-times-two is marked as :portageable which lets
;; portage know that it is asynchronous.
(def async-agent (agent nil))
(defn ^:portageable async-times-two
  [cc x]
  (send-off async-agent (fn [_]
                       (Thread/sleep 5000)
                       (cc (times-two x))
                       nil)))

;; Now let portage navigate the flow, managing the asynchronous operations
;; behind the scenes. The portage result function will put the result in
;; an atom so that we can examine it.
(def result-atom (atom nil))

(-+-> #(reset! result-atom %) 
      2
      times-two
      async-times-two
      times-two
      async-times-two
      times-two)

;; The result atom is still nil immediately after the return from the
;; -+-> form:
;;
;; user> @result-atom
;; nil
;;
;; 10 seconds later, the result has found its way through the flow:
;;
;; user> @result-atom
;; 64
```

## To Do
- Make `-+->>`, equivalent to `->>`
- Monadic (?) handling and propagation of errors down the flow
- Parallel flows
- Convenience wrapper block waiting for final result

## License

Copyright (c) 2012 Greg Spurrier

Distributed under the MIT license. See LICENSE.txt for the details.
