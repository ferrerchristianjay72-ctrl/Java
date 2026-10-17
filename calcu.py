
import ast
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
    """Evaluate numeric arithmetic without running arbitrary Python code."""
    tree = ast.parse(expression, mode="eval")

    def visit(node):
        if isinstance(node, ast.Expression):
            return visit(node.body)
        if isinstance(node, ast.Constant) and type(node.value) in (int, float):
            return node.value
        if isinstance(node, ast.BinOp) and type(node.op) in _BINARY_OPERATORS:
            return _BINARY_OPERATORS[type(node.op)](visit(node.left), visit(node.right))
        if isinstance(node, ast.UnaryOp) and type(node.op) in _UNARY_OPERATORS:
            return _UNARY_OPERATORS[type(node.op)](visit(node.operand))
        raise ValueError("Only numeric arithmetic (+, -, *, /, //, %, **) is supported.")

    return visit(tree)


class CalculatorApp:
    BG = "#000000"
    DIGIT = "#333333"
    UTILITY = "#a5a5a5"
    OPERATOR = "#ffb800"
    TEXT = "#ffffff"
    MUTED = "#8e8e93"

    def __init__(self, root):
        self.root = root
        self.root.title("Calculator")
        self.root.geometry("390x690")
        self.root.minsize(330, 570)
        self.root.configure(bg=self.BG)

        self.expression = tk.StringVar()
        self.status = tk.StringVar(value="Calculator")
        self._build_interface()

        self.display.bind("<Return>", lambda _event: self._evaluate())
        self.root.bind("<Escape>", lambda _event: self._clear())
        self.display.focus_set()

    def _build_interface(self):
        outer = tk.Frame(self.root, bg=self.BG, padx=18, pady=20)
        outer.pack(fill="both", expand=True)

        tk.Label(
            outer,
            text="Calculator",
            bg=self.BG,
            fg=self.MUTED,
            font=("Arial", 12),
        ).pack(anchor="center", pady=(4, 0))

        display_area = tk.Frame(outer, bg=self.BG)
        display_area.pack(fill="both", expand=True, pady=(18, 18))

        tk.Label(
            display_area,
            textvariable=self.status,
            bg=self.BG,
            fg=self.MUTED,
            font=("Arial", 12),
            anchor="e",
        ).pack(fill="x", pady=(0, 10))

        self.display = tk.Entry(
            display_area,
            textvariable=self.expression,
            justify="right",
            font=("Arial", 40),
            bg=self.BG,
            fg=self.TEXT,
            insertbackground=self.TEXT,
            relief="flat",
            bd=0,
        )
        self.display.pack(fill="x", ipady=8, padx=2)

        keypad = tk.Frame(outer, bg=self.BG)
        keypad.pack(fill="x", side="bottom")
        for column in range(4):
            keypad.grid_columnconfigure(column, weight=1, uniform="key")
        for row in range(5):
            keypad.grid_rowconfigure(row, weight=1, uniform="key")

        keys = [
            [("AC", "clear"), ("±", "utility"), ("%", "utility"), ("÷", "operator")],
            [("7", "digit"), ("8", "digit"), ("9", "digit"), ("×", "operator")],
            [("4", "digit"), ("5", "digit"), ("6", "digit"), ("−", "operator")],
            [("1", "digit"), ("2", "digit"), ("3", "digit"), ("+", "operator")],
            [("0", "digit"), (".", "digit"), ("=", "operator")],
        ]

        for row_index, row in enumerate(keys):
            column_index = 0
            for label, kind in row:
                span = 2 if label == "0" else 1
                self._add_button(keypad, label, kind, row_index, column_index, span)
                column_index += span

    def _add_button(self, parent, label, kind, row, column, span=1):
        colors = {
            "digit": self.DIGIT,
            "operator": self.OPERATOR,
            "utility": self.UTILITY,
            "clear": self.UTILITY,
        }
        foreground = "#000000" if kind in {"utility", "clear"} else self.TEXT
        button = tk.Canvas(
            parent,
            width=72,
            height=72,
            bg=self.BG,
            highlightthickness=0,
            bd=0,
            cursor="hand2",
        )
        button.grid(
            row=row,
            column=column,
            columnspan=span,
            sticky="nsew",
            padx=2,
            pady=4,
        )

        def draw_button(_event=None):
            button.delete("all")
            width = max(button.winfo_width(), 1)
            height = max(button.winfo_height(), 1)
            diameter = max(1, min(height - 4, width - 4))
            top = (height - diameter) / 2

            if label == "0":
                left = 2
                right = width - 2
                radius = diameter / 2
                button.create_rectangle(
                    left + radius, top, right - radius, top + diameter,
                    fill=colors[kind], outline=""
                )
                button.create_oval(
                    left, top, left + diameter, top + diameter,
                    fill=colors[kind], outline=""
                )
                button.create_oval(
                    right - diameter, top, right, top + diameter,
                    fill=colors[kind], outline=""
                )
                text_x = width / 2
            else:
                left = (width - diameter) / 2
                button.create_oval(
                    left, top, left + diameter, top + diameter,
                    fill=colors[kind], outline=""
                )
                text_x = width / 2

            font_size = max(15, min(22, int(diameter * 0.34)))
            inset = 3
            button.create_rectangle(
                inset,
                inset,
                max(inset + 1, width - inset),
                max(inset + 1, height - inset),
                fill=colors[kind],
                outline="",
            )
            font_size = max(15, min(22, int(min(height, width / span) * 0.34)))
            button.create_text(
                text_x,
                width / 2,
                height / 2,
                text=label,
                fill=foreground,
                font=("Arial", font_size, "bold"),
            )

        button.bind("<Configure>", draw_button)
        button.bind("<Button-1>", lambda _event, value=label: self._press(value))

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
        self.status.set("Calculator")
        self.display.focus_set()

    def _clear(self):
        self.expression.set("")
        self.status.set("Calculator")
        self.display.focus_set()

    def _toggle_sign(self):
        value = self.expression.get().strip()
        if not value:
            self.expression.set("-")
        elif value.startswith("-"):
            self.expression.set(value[1:])
        else:
            self.expression.set("-" + value)
        self.display.focus_set()

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