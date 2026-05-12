import React, { useEffect, useMemo, useState } from "react";
import { AnimatePresence, motion } from "motion/react";

import { cn } from "@/lib/utils"

export function AnimatedListItem({
  children
}) {
  const animations = {
    initial: { y: -12, opacity: 0 },
    animate: { y: 0, opacity: 1 },
    exit: { opacity: 0, height: 0, marginBottom: 0 },
    transition: { duration: 0.35, ease: "easeOut" },
  }

  return (
    <motion.div {...animations} layout className="w-full">
      {children}
    </motion.div>
  );
}

export const AnimatedList = React.memo(({
  children,
  className,
  delay = 120,
  ...props
}) => {
  const [index, setIndex] = useState(0)
  const childrenArray = useMemo(() => React.Children.toArray(children), [children])

  useEffect(() => {
    let timeout = null

    if (index < childrenArray.length - 1) {
      timeout = setTimeout(() => {
        setIndex((prevIndex) => (prevIndex + 1) % childrenArray.length)
      }, delay)
    }

    return () => {
      if (timeout !== null) {
        clearTimeout(timeout)
      }
    };
  }, [index, delay, childrenArray.length])

  const itemsToShow = useMemo(() => {
    const result = childrenArray.slice(0, index + 1)
    return result
  }, [index, childrenArray])

  return (
    <div className={cn(`flex flex-col items-stretch gap-2`, className)} {...props}>
      <AnimatePresence>
        {itemsToShow.map((item) => (
          <AnimatedListItem key={(item).key}>
            {item}
          </AnimatedListItem>
        ))}
      </AnimatePresence>
    </div>
  );
})

AnimatedList.displayName = "AnimatedList"
