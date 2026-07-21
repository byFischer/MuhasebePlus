import Icon from '@/components/mp/Icon';
import { slotSizeToCardSize } from '@/lib/dashboardLayouts';

// Boş slot — tıklayınca widget picker açılır
export function EmptySlot({ slot, onAdd }) {
  return (
    <button
      type="button"
      className="slot empty"
      style={{ gridColumn: slot.col, gridRow: slot.row }}
      onClick={() => onAdd(slot.id)}
      title="Widget Ekle"
    >
      <div className="slot-empty-inner">
        <Icon name="plus" size={20} />
        <span>Widget Ekle</span>
      </div>
    </button>
  );
}

// Dolu slot — DashboardCard render için container; size slot'tan gelir
export function FilledSlot({ slot, children }) {
  const cardSize = slotSizeToCardSize(slot.size);
  return (
    <div
      className="slot filled"
      style={{ gridColumn: slot.col, gridRow: slot.row }}
      data-slot-size={cardSize}
    >
      {children}
    </div>
  );
}
