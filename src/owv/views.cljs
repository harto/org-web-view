(ns owv.views
  (:require [clojure.string :as str]
            [owv.date :refer [days-since days-until format-date]]
            [owv.todo :as t]
            [re-frame.core :refer [dispatch subscribe]]
            [reagent.core :as r]))

;; Configuration

(defn config-panel []
  (let [saved-url @(subscribe [:data-url])
        url (r/atom saved-url)]
    (fn []
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (dispatch [:update-data-url @url])
                            (dispatch [:close-config]))}
       [:div
        [:label {:for "todo-json-url"} "Data URL: "]
        [:input#todo-json-url {:type "text"
                               :value @url
                               :on-change #(reset! url (.-value (.-target %)))}]]
       [:div
        [:input {:type "submit"
                 :disabled (nil? @url)
                 :value "OK"}]
        [:button {:disabled (nil? saved-url)
                  :on-click #(do
                               (.preventDefault %)
                               (dispatch [:close-config]))}
         "Cancel"]]])))

(defn loading-indicator []
  "Loading ...")

;; To-dos

(defn linkify
  "Transform links in org format (e.g. [[http://example.com][click me]]) to
  anchor elements."
  [s]
  (->> s
       (re-seq #"[^\[\]]+|\[\[.+?\]\]")
       (map (fn [part]
              (if-let [[_ url label] (re-matches #"\[\[(.+)\]\[(.+)\]\]" part)]
                [:a {:href url :target "_blank" :key url} label]
                part)))))

(defn todo-group [tag]
  (let [todos @(subscribe [:tagged-todos tag])
        ready @(subscribe [:tagged-ready-todos tag])
        needing-attention @(subscribe [:tagged-todos-needing-attention tag])
        overdue @(subscribe [:tagged-overdue-todos tag])]
    [:details.todo-group
     [:summary.todo-group-header
      [:span.todo-tag tag]
      (cond
        (> (count overdue) 0) [:span.todo-group-status-indicator.overdue]
        (> (count needing-attention) 0) [:span.todo-group-status-indicator.needs-attention]
        (> (count ready) 0) [:span.todo-group-status-indicator.ready])]
     (for [{:keys [state headline created scheduled deadline] :as todo} todos]
       [:details.todo-item
        {:key headline
         ;; TODO: figure out which things we want to see here
         ;; (e.g. waiting, deferred?)
         :data-todo-state (str/lower-case state)
         :class [(if (t/stale? todo) "stale")
                 (cond (t/overdue? todo) "overdue"
                       (t/needs-attention? todo) "needs-attention"
                       (t/ready? todo) "ready")]}
        [:summary
         [:input.todo-checkbox {:type "checkbox"}]
         (linkify headline)
         (if created
           [:span.created "(" (days-since created) " days old)"])]
        [:div.todo-meta
         [:div "Created: " (if created (format-date created) "?")]
         (if scheduled [:div "Scheduled: " (format-date scheduled)])
         (if deadline [:div "Deadline: " (format-date deadline)])]])]))

(defn todo-list []
  (let [tags @(subscribe [:tags])]
    [:<>
     (for [tag tags]
       ^{:key tag} [todo-group tag])]))

;; App root

(defn footer []
  [:div.updated-at
   "Updated " [:span {:title @(subscribe [:last-updated-at])}
               (str @(subscribe [:last-updated-minutes-ago]) " minutes ago")]])

(defn root []
  (let [loading-todos? @(subscribe [:loading-todos?])
        config-panel-open? @(subscribe [:config-panel-open?])]
    [:<>
     [todo-list]
     (if loading-todos? [loading-indicator])
     (if config-panel-open? [config-panel])
     [footer]]))
