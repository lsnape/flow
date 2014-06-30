(ns flow.el
  (:require [flow.expand :refer [expand-macros]]
            [flow.parse :refer [parse-form]]
            [flow.compile :refer [compile-el]]
            [flow.render :refer [render-el]]))

(defn print-console-ln [& args]
  (binding [*out* System/out]
    (apply println args)))

(defmacro el [elem]
  (-> (expand-macros elem &env)
      (parse-form {:elem? true})
      (compile-el {:state-sym (gensym "state")
                   :old-state-sym (gensym "old-state")
                   :new-state-sym (gensym "new-state")
                   :updated-var-sym (gensym "updated-var")})
      (render-el)))

