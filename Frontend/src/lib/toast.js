// MuhasebePlus — toast adapter (wraps Chakra toaster)
import { toaster } from '@/components/ui/toaster';


export const toast = {
  ok:   (msg, opts = {}) => toaster.create({ title: msg, type: 'success',  duration: opts.duration || 3000 }),
  err:  (msg, opts = {}) => toaster.create({ title: msg, type: 'error',    duration: opts.duration || 5000 }),
  info: (msg, opts = {}) => toaster.create({ title: msg, type: 'info',     duration: opts.duration || 3000 }),
  warn: (msg, opts = {}) => toaster.create({ title: msg, type: 'warning',  duration: opts.duration || 4000 }),
  undo: (msg, onUndo) => toaster.create({
    title: msg,
    type: 'info',
    duration: 6000,
    action: { label: 'Geri Al', onClick: onUndo },
  }),
};
