function dividir(a: integer, b: integer): integer {
  try { return a / b; } catch (e) { print(e); return 0; }
}
let r = dividir(10, 0);
for (let i = 0; i < 3; i = i + 1) {
  try { if (i == 1) { continue; } print(i); } catch (e) { break; }
}
