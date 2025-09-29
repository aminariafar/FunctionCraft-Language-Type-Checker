# ✅ FunctionCraft Language Type Checker

This project extends the **FunctionCraft language toolchain** by implementing a **semantic analyzer and type checker**.  
It builds on the grammar (defined separately) and provides infrastructure to validate programs for type correctness and semantic rules.

---

## ✨ Key Contents

### Core Source (Java)
- **AST Nodes (`src/main/ast/nodes/`)**  
  Classes representing the abstract syntax tree of FunctionCraft programs, e.g. `Program`, `FunctionDeclaration`, `PatternDeclaration`.  
- **Visitor Interfaces (`visitor/`)**  
  Defines traversal logic over the AST using the visitor design pattern.  
- **Semantic Analyzer (`src/main/semantic/`)**  
  Implements type checking rules and semantic analysis (e.g., function signatures, pattern correctness, main function validation).  

### ANTLR Generated Code (`gen/`)
- Lexer, parser, visitor, and listener classes generated from the FunctionCraft grammar.  
- Files like `FunctionCraftLexer.java`, `FunctionCraftParser.java`, `FunctionCraftBaseVisitor.java`.  

### Outputs & Samples
- **Compiled classes** under `out/production/`.  
- **Sample input programs** (`in.fl`) and **expected outputs** (`out.txt`, `out3.txt`, `out11.txt`) to illustrate type checking results.  

---

## 🧱 Project Structure
```
FunctionCraft-Language-Type-Checker-main/
└── SemanticAnalyzer/
    ├── src/main/ast/            # AST node classes
    ├── src/main/semantic/       # Semantic analyzer & type checker
    ├── gen/main/grammar/        # ANTLR-generated parser/lexer
    ├── out/production/          # Compiled classes
    ├── in.fl                    # Sample input program
    ├── out.txt                  # Sample analysis result
    └── samples/                 # Additional test outputs
```

---

## 🎯 Educational Context
This project was developed as part of a **compiler construction exercise**.  
It focuses on **semantic analysis and type checking**, showing how high-level language constructs are validated for correctness after parsing.

---

📚 Made for research & learning — advancing from grammar design to full semantic validation in the FunctionCraft language.
