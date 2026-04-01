# 🚀 LDCA: Lightweight Detection & Classification App

### A Hybrid Payload Generation and Vulnerability Validation Framework

---

## 📌 Overview

LDCA (Lightweight Detection & Classification App) is an Android-based cyber security application designed to detect and classify malicious payloads in real time. The app leverages machine learning techniques to identify multiple types of web and application-layer vulnerabilities such as SQL Injection, Cross-Site Scripting (XSS), Path Traversal, and more.

The system is optimized for **edge deployment**, allowing it to run efficiently on Android devices using **TensorFlow Lite**, without requiring cloud connectivity.

---

## 🎯 Features

* 🔍 Real-time payload detection
* 📁 CSV file and URL-based dataset input
* 🧠 Machine Learning-based classification
* ⚡ Lightweight and fast inference (TFLite)
* 📊 Visualization (charts, tables)
* 📄 PDF report generation
* 📶 Works offline (no internet required)

---

## 🛠️ Technologies Used

* Java (Android Development)
* TensorFlow Lite
* Machine Learning (TF-IDF + Logistic Regression)
* Data Processing (NLP techniques)

---

## 🧠 Supported Vulnerabilities

* Path Traversal
* Local File Inclusion (LFI)
* Cross-Site Scripting (XSS)
* SQL Injection
* Command Injection
* Open Redirect
* XXE
* SSRF
* GraphQL Attacks
* API Abuse
* IDOR

---

## 🏗️ System Architecture

The LDCA system consists of multiple modules including input handling, preprocessing, feature extraction, machine learning classification, and visualization.

<img width="4433" height="1819" alt="ldca_arch" src="https://github.com/user-attachments/assets/9489b09f-81c7-47dd-bf7b-390bd51d4ce3" />

---

## ⚙️ Workflow

1. Input payload (CSV / URL / manual)
2. Preprocess data (cleaning, decoding, tokenization)
3. Convert into TF-IDF vectors
4. Apply ML model for classification
5. Generate prediction and confidence score
6. Display results and visualization

---

## 📊 Model Performance

| Model               | Accuracy (%) | F1-Score (%) |
| ------------------- | ------------ | ------------ |
| Naive Bayes         | 34.5         | 34.2         |
| Logistic Regression | 32.0         | 31.5         |
| Decision Tree       | 20.5         | 20.3         |
| Random Forest       | 21.3         | 20.5         |

<img width="2070" height="1094" alt="fig1_model_comparison" src="https://github.com/user-attachments/assets/24cbb32f-1ff3-45fd-907f-fe3d754b79d4" />


---

## 📱 App Screenshots

### 🔹 Home Screen

<img width="367" height="785" alt="1" src="https://github.com/user-attachments/assets/b6e96d9e-cfbb-4379-85f9-48f3f54985d9" />

---

### 🔹 Payload Input

<img width="360" height="783" alt="4" src="https://github.com/user-attachments/assets/4e703509-22e7-4bd7-a3c3-fd5d6a5ad0be" />

---

### 🔹 Detection Result

<img width="370" height="777" alt="5" src="https://github.com/user-attachments/assets/a8d2de17-fea7-4fef-a493-9cfbb16277ae" />

---

### 🔹 Visualization Output

<img width="352" height="760" alt="6" src="https://github.com/user-attachments/assets/26743f2d-5cef-458b-b92e-fc10b680e30d" />

---

## 📈 Dataset

* Total Samples: **89,256**
* Covers **11 vulnerability categories**
* Includes both malicious and benign payloads
* Designed to simulate real-world attack scenarios

---

## 🔬 Key Highlights

* Lightweight ML model suitable for mobile devices
* Efficient TF-IDF based feature extraction
* Offline functionality with TFLite deployment
* Handles encoded and obfuscated payloads effectively

---

## 🚧 Limitations

* Performance may vary for highly complex obfuscated payloads
* Limited samples for rare attack categories
* Model accuracy depends on dataset diversity

---
[LDCA.docx](https://github.com/user-attachments/files/26396119/LDCA.docx)
[Lightweight Data Classifier for Android.pptx](https://github.com/user-attachments/files/26396273/Lightweight.Data.Classifier.for.Android.pptx)




