(ns owv.views
  (:require [clojure.string :as str]
            [owv.date :refer [days-since days-until format-date]]
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

(defn todo-group [tag]
  [:details.todo-group
   [:summary.todo-tag tag]
   (let [todos @(subscribe [:get-todos tag])]
     (for [{:keys [state headline created scheduled deadline]} todos]
       [:details.todo-item
        {:key headline
         ;; TODO: figure out which things we want to see here
         ;; (e.g. waiting, deferred?)
         :data-todo-state (str/lower-case state)
         :data-stale (and (not scheduled) (> (days-since created) 30))
         :data-scheduled (let [days-since-scheduled (days-since scheduled)]
                           (cond (> days-since-scheduled 0) :overdue
                                 (= days-since-scheduled 0) :now))
         :data-deadline (let [days-until-deadline (days-until deadline)]
                          (cond (< days-until-deadline 0) :overdue
                                (< days-until-deadline 3) :now
                                (<= days-until-deadline 7) :soon))}
        [:summary
         [:input.todo-checkbox {:type "checkbox"}]
         headline
         (if created
           [:span.created "(" (days-since created) " days old)"])]
        [:div.todo-meta
         [:div "Created: " (if created (format-date created) "?")]
         (if scheduled [:div "Scheduled: " (format-date scheduled)])
         (if deadline [:div "Deadline: " (format-date deadline)])]]))])

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
