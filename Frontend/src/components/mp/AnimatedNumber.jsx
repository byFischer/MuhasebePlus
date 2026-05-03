import { useEffect, useState, useRef } from 'react';

export default function AnimatedNumber({ value, formatter, duration = 800, className }) {
  const [display, setDisplay] = useState(value);
  const prevValue = useRef(value);
  const formatterRef = useRef(formatter);

  // Her render'da güncel formatter'ı ref'e al
  formatterRef.current = formatter;

  useEffect(() => {
    const start = prevValue.current;
    const end = value;
    const startTime = performance.now();

    if (start === end) {
      setDisplay(end);
      return;
    }

    let rafId;

    function animate(now) {
      const elapsed = now - startTime;
      const progress = Math.min(elapsed / duration, 1);
      // easeOutQuart — hızlı başlangıç, yavaş bitiş (slot makinesi / para sayımı efekti)
      const ease = 1 - Math.pow(1 - progress, 4);
      const current = start + (end - start) * ease;
      setDisplay(current);

      if (progress < 1) {
        rafId = requestAnimationFrame(animate);
      } else {
        setDisplay(end);
        prevValue.current = end;
      }
    }

    rafId = requestAnimationFrame(animate);
    return () => cancelAnimationFrame(rafId);
  }, [value, duration]);

  const formatted = formatterRef.current
    ? formatterRef.current(display)
    : display;

  return <span className={className}>{formatted}</span>;
}
