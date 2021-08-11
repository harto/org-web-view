(ns owv.main
  (:require
   [owv.views :as views]
   [re-frame.core :as rf]
   [reagent.dom :as rdom]
   ;; load for side-effects
   [owv.events]
   [owv.subs]))

(defn ^:dev/after-load mount-root []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "root")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/root] root-el)))

(defn init []
  (rf/dispatch-sync [:init])
  (mount-root))
