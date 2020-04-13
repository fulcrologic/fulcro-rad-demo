(ns com.example.ui.file-forms
  (:require
    [com.example.model.file :as m.file]
    [com.fulcrologic.rad.form-options :as fo]
    [com.fulcrologic.rad.form :as form]))

(form/defsc-form FileForm [this props]
  {fo/id            m.file/id
   fo/layout-styles {:form-container :file-as-icon}
   fo/attributes    [m.file/uploaded-on
                     m.file/sha
                     m.file/filename]
   fo/field-labels  {:file.sha/filename ""
                     :file/uploaded-on  ""
                     :file/sha          ""}})
