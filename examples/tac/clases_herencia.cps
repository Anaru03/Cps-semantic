class Animal {
  let nombre: string = "x";
  let patas: integer = 4;
  function constructor(n: string) { this.nombre = n; }
  function hablar(): string { return "..."; }
  function describir(): integer { return this.patas + 1; }
}
class Perro : Animal {
  let raza: string;
  function constructor(n: string, r: string) { this.nombre = n; this.raza = r; }
  function hablar(): string { return "guau"; }
}
let a: Animal = new Perro("rex", "lab");
let s: string = a.hablar();
let xs: integer[] = [1, 2, 3];
xs[1] = xs[0] + 5;
let m: integer[][] = [[1,2],[3,4]];
let n = m[1][0] + xs.length;
let ok: boolean = n > 2 && xs[0] == 1 || false;
let v = ok ? 10 : 20;
try { let z = 10 / 0; print(z); } catch (e) { print(e); }
print(a.describir());
