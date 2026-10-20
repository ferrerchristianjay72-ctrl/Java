
import ast
import math
import operator
import tkinter as tk


_BINARY_OPERATORS = {
    ast.Add: operator.add,
    ast.Sub: operator.sub,
    ast.Mult: operator.mul,
    ast.Div: operator.truediv,
    ast.FloorDiv: operator.floordiv,
    ast.Mod: operator.mod,
    ast.Pow: operator.pow,
}
_UNARY_OPERATORS = {ast.UAdd: operator.pos, ast.USub: operator.neg}


def calculate(expression):
    """Evaluate a basic arithmetic expression without using eval()."""
    expression = expression.strip()
    if not expression or len(expression) > 256:
        raise ValueError("Enter a shorter arithmetic expression.")

    tree = ast.parse(expression, mode="eval")

    def visit(node):
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and type(node.value) in (int, float):
            return node.value
        if isinstance(node, ast.BinOp) and type(node.op) in _BINARY_OPERATORS:
            left = visit(node.left)
            right = visit(node.right)
            if isinstance(node.op, ast.Pow) and abs(right) > 100:
                raise ValueError("The exponent is too large.")
            result = _BINARY_OPERATORS[type(node.op)](left, right)
            if type(result) not in (int, float):
                raise ValueError("The result must be a real number.")
            if isinstance(result, float) and not math.isfinite(result):
                raise ValueError("The result is too large.")
            return result
        if isinstance(node, ast.UnaryOp) and type(node.op) in _UNARY_OPERATORS:
            return _UNARY_OPERATORS[type(node.op)](visit(node.operand))
        raise ValueError("Use numbers and the operators +, −, ×, ÷, %, //, or **.")

    return visit(tree)


class CalculatorApp:
    BG = "#0b1220"
    PANEL = "#111c2e"
    DIGIT = "#1a2940"
    UTILITY = "#293951"
    ACCENT = "#20c7a2"
    TEXT = "#f4f7fb"
    MUTED = "#8fa2bb"

    KEYS = [
        [("AC", "utility"), ("±", "utility"), ("%", "utility"), ("÷", "operator")],
        [("7", "digit"), ("8", "digit"), ("9", "digit"), ("×", "operator")],
        [("4", "digit"), ("5", "digit"), ("6", "digit"), ("−", "operator")],
        [("1", "digit"), ("2", "digit"), ("3", "digit"), ("+", "operator")],
        [("0", "digit"), (".", "digit"), ("=", "equals")],
    ]

    def __init__(self, root):
        self.root = root
        self.expression = tk.StringVar()
        self.status = tk.StringVar(value="Type an expression")

        root.title("Calculator")
        root.geometry("400x650")
        root.minsize(340, 560)
        root.configure(bg=self.BG)

        self._build_interface()
        self.display.bind("<Return>", self._evaluate_event)
        root.bind("<Escape>", lambda _event: self._clear())
        self.display.focus_set()

    def _build_interface(self):
        container = tk.Frame(self.root, bg=self.BG, padx=20, pady=22)
        container.pack(fill="both", expand=True)

        tk.Label(
            container,
            text="CALCULATOR",
            bg=self.BG,
            fg=self.MUTED,
            font=("Segoe UI", 11, "bold"),
        ).pack(anchor="w", pady=(0, 14))

        display_panel = tk.Frame(container, bg=self.PANEL, padx=16, pady=14)
        display_panel.pack(fill="x", pady=(0, 18))

        tk.Label(
            display_panel,
            textvariable=self.status,
            bg=self.PANEL,
            fg=self.MUTED,
            font=("Segoe UI", 10),
            anchor="e",
        ).pack(fill="x", pady=(0, 8))

        self.display = tk.Entry(
            display_panel,
            textvariable=self.expression,
            justify="right",
            font=("Segoe UI", 32),
            bg=self.PANEL,
            fg=self.TEXT,
            insertbackground=self.ACCENT,
            selectbackground=self.ACCENT,
            selectforeground=self.BG,
            relief="flat",
            bd=0,
        )
        self.display.pack(fill="x", ipady=7)

        keypad = tk.Frame(container, bg=self.BG)
        keypad.pack(fill="both", expand=True)
        for column in range(4):
            keypad.grid_columnconfigure(column, weight=1, uniform="key")
        for row in range(len(self.KEYS)):
            keypad.grid_rowconfigure(row, weight=1, uniform="key")

        for row_index, row in enumerate(self.KEYS):
            column_index = 0
            for label, kind in row:
                span = 2 if label == "0" else 1
                self._add_button(keypad, label, kind, row_index, column_index, span)
                column_index += span

    def _add_button(self, parent, label, kind, row, column, span=1):
        palette = {
            "digit": (self.DIGIT, "#263954", self.TEXT),
            "utility": (self.UTILITY, "#364b68", self.TEXT),
            "operator": (self.UTILITY, "#364b68", self.ACCENT),
            "equals": (self.ACCENT, "#45d7b7", self.BG),
        }
        background, active_background, foreground = palette[kind]
        button = tk.Button(
            parent,
            text=label,
            command=lambda value=label: self._press(value),
            bg=background,
            fg=foreground,
            activebackground=active_background,
            activeforeground=foreground,
            disabledforeground=self.MUTED,
            relief="flat",
            bd=0,
            highlightthickness=0,
            cursor="hand2",
            font=("Segoe UI", 18, "bold"),
            takefocus=False,
        )
        button.grid(
            row=row,
            column=column,
            columnspan=span,
            sticky="nsew",
            padx=5,
            pady=5,
            ipady=8,
        )

    def _press(self, label):
        if label == "AC":
            self._clear()
            return
        if label == "=":
            self._evaluate()
            return
        if label == "±":
            self._toggle_sign()
            return

        symbols = {"×": "*", "÷": "/", "−": "-"}
        self.expression.set(self.expression.get() + symbols.get(label, label))
        self.status.set("Type an expression")
        self.display.focus_set()

    def _clear(self):
        self.expression.set("")
        self.status.set("Type an expression")
        self.display.focus_set()

    def _toggle_sign(self):
        value = self.expression.get().strip()
        if value.startswith("-"):
            value = value[1:]
        elif value:
            value = "-" + value
        else:
            value = "-"
        self.expression.set(value)
        self.display.focus_set()

    def _evaluate_event(self, _event):
        self._evaluate()
        return "break"

    def _evaluate(self):
        value = self.expression.get().strip()
        if not value:
            self.status.set("Enter an expression first")
            return

        try:
            result = calculate(value)
            if isinstance(result, float) and result.is_integer():
                result = int(result)
            self.expression.set(str(result))
            self.status.set("Result")
        except (SyntaxError, ValueError, ZeroDivisionError, OverflowError, TypeError):
            self.status.set("Invalid expression — check your input")
        self.display.focus_set()


def main():
    root = tk.Tk()
    CalculatorApp(root)
    root.mainloop()


if __name__ == "__main__":
    main()
