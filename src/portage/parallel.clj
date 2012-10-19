(ns portage.parallel
  "Parallel execution of portageable functions"
  (:require [portage.core :refer (-+->>)]))

;; ## Accumulating Results
;; 

(defn- assoc-result
  [result idx x]
  (-> result
      (assoc-in [:values idx] x)
      (update-in [:num-remaining] dec)))

(defn- make-result-setter
  [result idx k]
  (fn [x]
    (let [new-result (swap! result assoc-result idx x)]
      (if (= (:num-remaining new-result) 0)
        (k (:values new-result))))))

(defn ^:portageable map-p
  "A portageable equivalent of map. Invokes pf--which must be
  portageable--on each element of coll. Invokes the continuation with
  a vector of the results once they are available."
  [k pf coll]
  (let [pairs      (map vector coll (iterate inc 0))
        call-count (count pairs)
        result     (atom {:num-remaining call-count
                          :values        (into [] (repeat call-count nil))})]
    ;; Fire off the async calls
    (doseq [[x idx] pairs]
      (pf (make-result-setter result idx k) x))))

(defn ^:portageable mapcat-p
  "A portageable equivalent of mapcat. Invokes pf--which must be
porteagable--on each element of coll. The results, assumed to be
collections, are then concatenated and passed to the continuation as a
seq."
  [k pf coll]
  (-+->> coll
         (map-p pf)
         (apply concat)
         k))

(defn ^:portageable forkv
  [k x & pfs]
  (let [pairs      (map vector pfs (iterate inc 0))
        call-count (count pairs)
        result     (atom {:num-remaining call-count
                          :values        (into [] (repeat call-count nil))})]
    ;; Fire off the async calls
    (doseq [[pf idx] pairs]
      (pf (make-result-setter result idx k) x))))

(defn ^:portageable forkm
  [k x & {:as pf-map}]
  (let [pfs (vals pf-map)]
    (apply forkv
           (fn [valuess] (k (zipmap (keys pf-map) valuess)))
           x pfs)))