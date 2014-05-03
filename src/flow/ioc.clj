(ns flow.ioc
  (:require [clojure.walk :refer [postwalk macroexpand-all]]
            [flow.stream :refer [stream-bind ->stream]]))

(defn find-streams [form]
  (cond

   (and (list? form) (= '<< (first form)))
   (let [stream-sym (gensym "stream")]
     (-> stream-sym
         (with-meta {:streams (conj (mapcat (comp :streams meta) form)
                                    {:stream (second form)
                                     :stream-sym stream-sym})})))

   (map? form) (-> form
                   (with-meta {:streams (->> form
                                             ((juxt keys vals))
                                             (apply concat)
                                             (mapcat (comp :streams meta)))}))
   
   (coll? form) (-> form
                    (with-meta {:streams (mapcat (comp :streams meta) form)}))
   
   :else form))

(defmacro form->stream [form]
  (let [form (postwalk find-streams (macroexpand-all form))
        streams (:streams (meta form))]
    
    (reduce (fn [form {:keys [stream-sym stream]}]
              `(stream-bind (->stream ~stream)
                            (fn [~stream-sym]
                              ~form)))
            form
            streams)))

