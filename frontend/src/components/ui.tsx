import { AlertCircle, CheckCircle2, Info, Loader2, X } from 'lucide-react';
import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { COR_RISCO_META } from '../lib/format';
import type { CorRisco } from '../types/api';

export function Card({
  children,
  className = '',
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={`bg-white rounded-xl border border-slate-200 shadow-xs ${className}`}>
      {children}
    </div>
  );
}

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost';
  size?: 'sm' | 'md';
  loading?: boolean;
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  className = '',
  children,
  ...rest
}: ButtonProps) {
  const base =
    'inline-flex items-center justify-center gap-2 font-medium rounded-lg transition-colors focus:outline-none focus:ring-2 focus:ring-offset-1 disabled:opacity-50 disabled:cursor-not-allowed cursor-pointer';
  const sizes = {
    sm: 'px-3 py-1.5 text-xs',
    md: 'px-4 py-2 text-sm',
  }[size];
  const variants = {
    primary: 'bg-teal-600 text-white hover:bg-teal-700 focus:ring-teal-500',
    secondary:
      'bg-white text-slate-700 border border-slate-300 hover:bg-slate-50 focus:ring-slate-400',
    danger: 'bg-red-600 text-white hover:bg-red-700 focus:ring-red-500',
    ghost: 'text-slate-600 hover:bg-slate-100 hover:text-slate-900 focus:ring-slate-400',
  }[variant];

  return (
    <button
      disabled={disabled || loading}
      className={`${base} ${sizes} ${variants} ${className}`}
      {...rest}
    >
      {loading && <Loader2 className="w-4 h-4 animate-spin" />}
      {children}
    </button>
  );
}

export function AlertBanner({
  type = 'error',
  message,
  onClose,
}: {
  type?: 'error' | 'success' | 'info' | 'warning';
  message: string;
  onClose?: () => void;
}) {
  const styles = {
    error: 'bg-red-50 border-red-200 text-red-800',
    success: 'bg-emerald-50 border-emerald-200 text-emerald-800',
    info: 'bg-sky-50 border-sky-200 text-sky-800',
    warning: 'bg-amber-50 border-amber-200 text-amber-900',
  }[type];

  const Icon = type === 'success' ? CheckCircle2 : type === 'info' ? Info : AlertCircle;

  return (
    <div className={`flex items-start gap-3 px-4 py-3 rounded-lg border text-sm ${styles}`}>
      <Icon className="w-4 h-4 mt-0.5 shrink-0" />
      <span className="flex-1">{message}</span>
      {onClose && (
        <button
          type="button"
          onClick={onClose}
          className="opacity-60 hover:opacity-100 cursor-pointer"
          aria-label="Fechar aviso"
        >
          <X className="w-4 h-4" />
        </button>
      )}
    </div>
  );
}

export function ManchesterBadge({
  cor,
  showTempo = false,
}: {
  cor: CorRisco | null | undefined;
  showTempo?: boolean;
}) {
  if (!cor) {
    return (
      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 text-slate-600">
        Sem classificação
      </span>
    );
  }
  const meta = COR_RISCO_META[cor];
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-xs font-semibold ${meta.badge}`}
    >
      {meta.label}
      {showTempo && <span className="opacity-85 font-normal">({meta.tempo})</span>}
    </span>
  );
}

export function Modal({
  open,
  onClose,
  title,
  children,
  maxWidth = 'max-w-lg',
}: {
  open: boolean;
  onClose: () => void;
  title: string;
  children: ReactNode;
  maxWidth?: string;
}) {
  if (!open) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/50 backdrop-blur-[1px]">
      <div
        className={`bg-white rounded-xl shadow-xl border border-slate-200 w-full ${maxWidth} max-h-[90vh] flex flex-col`}
      >
        <div className="flex items-center justify-between px-5 py-4 border-b border-slate-200">
          <h3 className="text-base font-semibold text-slate-900">{title}</h3>
          <button
            type="button"
            onClick={onClose}
            className="text-slate-400 hover:text-slate-600 cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>
        <div className="p-5 overflow-y-auto">{children}</div>
      </div>
    </div>
  );
}

export function Spinner({ label = 'Carregando...' }: { label?: string }) {
  return (
    <div className="flex items-center justify-center gap-2 py-12 text-slate-500 text-sm">
      <Loader2 className="w-5 h-5 animate-spin text-teal-600" />
      <span>{label}</span>
    </div>
  );
}

export function EmptyState({
  title,
  description,
  action,
}: {
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="text-center py-12 px-4 bg-white rounded-xl border border-dashed border-slate-300">
      <p className="text-sm font-medium text-slate-800">{title}</p>
      {description && <p className="text-xs text-slate-500 mt-1">{description}</p>}
      {action && <div className="mt-4">{action}</div>}
    </div>
  );
}
