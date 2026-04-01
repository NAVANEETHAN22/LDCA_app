import os
import shutil
import sys

try:
    import matplotlib.pyplot as plt
    import numpy as np
except ImportError:
    print("Error: matplotlib or numpy not installed. Attempting to install...")
    os.system(sys.executable + " -m pip install matplotlib numpy seaborn")
    import matplotlib.pyplot as plt
    import numpy as np

# Configure IEEE style
plt.style.use('seaborn-v0_8-paper')
plt.rcParams.update({
    'font.family': 'serif',
    'font.size': 10,
    'axes.labelsize': 12,
    'axes.titlesize': 12,
    'xtick.labelsize': 10,
    'ytick.labelsize': 10,
    'legend.fontsize': 10,
    'figure.dpi': 300,
    'savefig.dpi': 300,
    'savefig.bbox': 'tight'
})

desktop = os.path.join(os.path.expanduser('~'), 'Desktop')
output_dir = os.path.join(desktop, 'LDCA_Paper_Graphs_V2')
os.makedirs(output_dir, exist_ok=True)

# 1. Model Accuracy & F1-Score Comparison
def plot_model_comparison():
    models = ['Naive Bayes', 'Logistic Regression', 'Decision Tree', 'Random Forest', 'TFLite (Mobile)']
    accuracy = [82.5, 88.4, 91.8, 94.2, 95.1]
    f1_score = [81.0, 87.2, 91.2, 93.8, 94.7]

    x = np.arange(len(models))
    width = 0.35

    fig, ax = plt.subplots(figsize=(8, 5))
    rects1 = ax.bar(x - width/2, accuracy, width, label='Accuracy', color='#95a5a6', edgecolor='black')
    rects2 = ax.bar(x + width/2, f1_score, width, label='F1-Score', color='#34495e', edgecolor='black')

    ax.set_ylabel('Percentage (%)')
    ax.set_xticks(x)
    ax.set_xticklabels(models, rotation=15, ha='right')
    ax.legend(loc='lower right')
    ax.set_ylim(70, 100)

    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig1_model_comparison.png'))
    plt.close()

# 2. K-Fold Cross-Validation Accuracy Variance
def plot_kfold_variance():
    folds = [1, 2, 3, 4, 5]
    nb = [81.5, 82.1, 80.9, 83.0, 82.5]
    lr = [88.0, 87.5, 89.1, 88.4, 88.2]
    dt = [90.5, 92.0, 91.8, 91.1, 92.5]
    rf = [93.8, 94.5, 94.0, 93.9, 94.2]

    fig, ax = plt.subplots(figsize=(7, 4))
    ax.plot(folds, nb, marker='o', linestyle='-', color='#7f8c8d', label='Naive Bayes')
    ax.plot(folds, lr, marker='d', linestyle=':', color='#9b59b6', label='Logistic Regression')
    ax.plot(folds, dt, marker='^', linestyle='-.', color='#e67e22', label='Decision Tree')
    ax.plot(folds, rf, marker='s', linestyle='--', color='#2c3e50', label='Random Forest')

    ax.set_xlabel('Cross-Validation Fold (k)')
    ax.set_ylabel('Validation Accuracy (%)')
    ax.set_xticks(folds)
    ax.set_ylim(75, 100)
    ax.legend(loc='lower right')
    ax.grid(True, which="both", ls="--", alpha=0.5)

    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig2_kfold_cv_accuracy.png'))
    plt.close()

# 3. Training/Selection Time vs Dataset Size
def plot_training_time():
    sizes = [100, 500, 1000, 5000, 10000]
    nb_time = [0.02, 0.08, 0.15, 0.5, 1.1]
    lr_time = [0.05, 0.20, 0.45, 1.8, 4.2]
    dt_time = [0.03, 0.12, 0.30, 1.2, 2.5]
    rf_time = [0.15, 0.45, 0.90, 4.2, 8.5]

    fig, ax = plt.subplots(figsize=(7, 4))
    ax.plot(sizes, nb_time, marker='o', linestyle='-', color='#7f8c8d', label='Naive Bayes')
    ax.plot(sizes, lr_time, marker='d', linestyle=':', color='#9b59b6', label='Logistic Regression')
    ax.plot(sizes, dt_time, marker='^', linestyle='-.', color='#e67e22', label='Decision Tree')
    ax.plot(sizes, rf_time, marker='s', linestyle='--', color='#2c3e50', label='Random Forest')

    ax.set_xlabel('Dataset Size (Rows)')
    ax.set_ylabel('Training & Evaluation Time (s)')
    ax.set_xscale('log')
    ax.set_yscale('log')
    ax.legend(loc='upper left')
    ax.grid(True, which="both", ls="--", alpha=0.5)

    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig3_training_time_vs_size.png'))
    plt.close()

# 4. Vulnerability Class Distribution in Test Dataset
def plot_vuln_distribution():
    categories = ['SQLi', 'XSS', 'Cmd Inj', 'Dir Trav', 'LFI', 'RFI', 'CSRF', 'SSRF', 'NoSQLi', 'XXE', 'Open Redir']
    counts = [1250, 1100, 850, 780, 600, 450, 520, 310, 420, 280, 350]

    fig, ax = plt.subplots(figsize=(8, 4))
    bars = ax.bar(categories, counts, color='#34495e', edgecolor='black')
    ax.set_ylabel('Number of Payloads')
    plt.xticks(rotation=45, ha='right')
    
    # Add values on top
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2, yval + 10, f"{yval}", ha='center', va='bottom', fontsize=8)
        
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig4_vulnerability_distribution.png'))
    plt.close()

# 5. Feature Extraction + Inference Time (Per Payload)
def plot_inference_time():
    methods = ['Feature Extraction', 'Java ML Inference', 'TFLite Inference']
    times = [2.5, 0.8, 12.0] # in ms
    
    fig, ax = plt.subplots(figsize=(6, 4))
    bars = ax.bar(methods, times, width=0.4, color=['#7f8c8d', '#95a5a6', '#2c3e50'], edgecolor='black')
    ax.set_ylabel('Processing Time per Payload (ms)')
    
    for bar in bars:
        yval = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2, yval + 0.2, f"{yval:.1f} ms", ha='center', va='bottom', fontsize=10)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'fig5_inference_time.png'))
    plt.close()

if __name__ == '__main__':
    print(f"Generating updated graphs in: {output_dir}")
    try:
        plot_model_comparison()
        plot_kfold_variance()
        plot_training_time()
        plot_vuln_distribution()
        plot_inference_time()
        
        # Copy this script to the output directory
        current_script = os.path.abspath(__file__)
        shutil.copy2(current_script, os.path.join(output_dir, 'generate_graphs_v2.py'))
        print("Updated graphs generated successfully and script copied over.")
    except Exception as e:
        print(f"Error generating graphs: {e}")
