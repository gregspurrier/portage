(ns portage.core)

(defmacro portage->
  [x f & forms]
  `(do (~f (-> ~x ~@forms))
       nil))