import os
import glob

template_dir = r"d:\latest mid term homeease\new latest mid\homeease_mid\src\main\resources\templates"

html_files = glob.glob(os.path.join(template_dir, "**/*.html"), recursive=True)

snippet = '\n<div th:replace="common/translator :: translator"></div>\n'

count = 0
for fpath in html_files:
    if "common" in fpath:
        continue
    
    with open(fpath, "r", encoding="utf-8") as f:
        content = f.read()
        
    if "</body>" in content and "common/translator :: translator" not in content:
        content = content.replace("</body>", snippet + "</body>")
        with open(fpath, "w", encoding="utf-8") as f:
            f.write(content)
        count += 1
        print(f"Injected translator into {fpath}")

print(f"Done. Injected into {count} files.")
