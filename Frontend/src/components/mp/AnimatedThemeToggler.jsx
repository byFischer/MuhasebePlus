import { useTheme } from "next-themes";
import { useEffect, useState } from "react";
import { Sun, Moon } from "lucide-react";

export default function AnimatedThemeToggler({ size = 36 }) {
  const { resolvedTheme, setTheme } = useTheme();
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
  }, []);

  const toggleTheme = (e) => {
    const isDark = resolvedTheme === "dark";
    const next = isDark ? "light" : "dark";

    // Fallback for browsers without View Transitions API
    if (!document.startViewTransition) {
      setTheme(next);
      return;
    }

    const x = e.clientX;
    const y = e.clientY;
    const endRadius = Math.hypot(
      Math.max(x, window.innerWidth - x),
      Math.max(y, window.innerHeight - y)
    );

    const transition = document.startViewTransition(() => {
      const root = document.documentElement;
      if (next === "dark") {
        root.classList.add("dark");
        root.setAttribute("data-theme", "dark");
      } else {
        root.classList.remove("dark");
        root.setAttribute("data-theme", "light");
      }
    });

    // React state'i sonradan senkronize et
    requestAnimationFrame(() => {
      setTheme(next);
    });

    transition.ready.then(() => {
      document.documentElement.animate(
        {
          clipPath: [
            `circle(0px at ${x}px ${y}px)`,
            `circle(${endRadius}px at ${x}px ${y}px)`,
          ],
        },
        {
          duration: 500,
          easing: "ease-in",
          pseudoElement: "::view-transition-new(root)",
        }
      );
    });
  };

  if (!mounted) {
    return (
      <button className="tb-icon-btn" title="Tema" style={{ width: size, height: size }}>
        <Moon size={Math.max(14, size * 0.4)} />
      </button>
    );
  }

  const isDark = resolvedTheme === "dark";

  return (
    <button
      className="tb-icon-btn"
      title="Tema"
      onClick={toggleTheme}
      style={{
        width: size,
        height: size,
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        overflow: "hidden",
      }}
    >
      <span
        key={isDark ? "moon" : "sun"}
        style={{
          display: "inline-flex",
          animation: "togSpin 0.4s ease",
        }}
      >
        {isDark ? (
          <Moon size={Math.max(14, size * 0.4)} />
        ) : (
          <Sun size={Math.max(14, size * 0.4)} />
        )}
      </span>
    </button>
  );
}
