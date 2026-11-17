import { ChevronLeft, ChevronRight, Search, ShieldCheck } from 'lucide-react';
import { Fragment, useCallback, useEffect, useState, type FormEvent } from 'react';
import { AlertBanner, Button, Card, EmptyState, Spinner } from '../components/ui';
import { ApiError, auditoriaApi } from '../lib/api';
import { formatDateTime } from '../lib/format';
import type { LogAuditoriaDto, Page } from '../types/api';

export function AuditoriaPage() {
  const [pageData, setPageData] = useState<Page<LogAuditoriaDto> | null>(null);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [usuario, setUsuario] = useState('');
  const [recurso, setRecurso] = useState('');
  const [page, setPage] = useState(0);
  const [expandidoId, setExpandidoId] = useState<number | null>(null);

  const carregar = useCallback(
    async (pagina = 0) => {
      setLoading(true);
      setErro(null);
      try {
        const res = await auditoriaApi.listar({
          usuario: usuario.trim() || undefined,
          recurso: recurso.trim() || undefined,
          page: pagina,
          size: 20,
        });
        setPageData(res);
        setPage(res.number);
      } catch (err) {
        setErro(err instanceof ApiError ? err.message : 'Erro ao carregar trilha de auditoria.');
      } finally {
        setLoading(false);
      }
    },
    [usuario, recurso],
  );

  useEffect(() => {
    void carregar(0);
  }, [carregar]);

  function handleFiltrar(e: FormEvent) {
    e.preventDefault();
    void carregar(0);
  }

  return (
    <div className="space-y-5">
      <div>
        <h2 className="text-xl font-bold text-slate-900 flex items-center gap-2">
          <ShieldCheck className="w-5 h-5 text-teal-600" />
          Trilha de Auditoria Imutável
        </h2>
        <p className="text-sm text-slate-500">
          Registro de operações críticas, acessos a prontuário e uso de IA (RN-006)
        </p>
      </div>

      {erro && <AlertBanner type="error" message={erro} onClose={() => setErro(null)} />}

      <Card className="p-4">
        <form onSubmit={handleFiltrar} className="grid grid-cols-1 sm:grid-cols-3 gap-3">
          <input
            type="text"
            placeholder="Filtrar por e-mail do usuário..."
            value={usuario}
            onChange={(e) => setUsuario(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm"
          />
          <input
            type="text"
            placeholder="Recurso (ex.: Paciente, Triagem, Prescricao)"
            value={recurso}
            onChange={(e) => setRecurso(e.target.value)}
            className="px-3 py-2 border border-slate-300 rounded-lg text-sm"
          />
          <div className="flex gap-2">
            <Button type="submit" variant="secondary" className="flex-1">
              <Search className="w-4 h-4" />
              Filtrar
            </Button>
            {(usuario || recurso) && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => {
                  setUsuario('');
                  setRecurso('');
                }}
              >
                Limpar
              </Button>
            )}
          </div>
        </form>
      </Card>

      {loading ? (
        <Spinner />
      ) : !pageData || pageData.content.length === 0 ? (
        <EmptyState title="Nenhum registro encontrado" />
      ) : (
        <Card className="overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-xs">
              <thead>
                <tr className="bg-slate-50 border-b border-slate-200 text-slate-600">
                  <th className="py-3 px-4 font-semibold">Data/Hora</th>
                  <th className="py-3 px-4 font-semibold">Usuário</th>
                  <th className="py-3 px-4 font-semibold">Ação</th>
                  <th className="py-3 px-4 font-semibold">Recurso</th>
                  <th className="py-3 px-4 font-semibold">ID</th>
                  <th className="py-3 px-4 font-semibold text-right">Detalhes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {pageData.content.map((log) => {
                  const aberto = expandidoId === log.id;
                  return (
                    <Fragment key={log.id}>
                      <tr className="hover:bg-slate-50/70">
                        <td className="py-2.5 px-4 whitespace-nowrap text-slate-600">
                          {formatDateTime(log.dataHora)}
                        </td>
                        <td className="py-2.5 px-4 font-medium text-slate-800">{log.usuario}</td>
                        <td className="py-2.5 px-4">
                          <span className="px-2 py-0.5 rounded bg-slate-100 text-slate-800 font-mono">
                            {log.acao}
                          </span>
                        </td>
                        <td className="py-2.5 px-4 text-slate-700">{log.recurso}</td>
                        <td className="py-2.5 px-4 text-slate-500">{log.idRecurso ?? '—'}</td>
                        <td className="py-2.5 px-4 text-right">
                          {log.detalhes && Object.keys(log.detalhes).length > 0 ? (
                            <button
                              type="button"
                              onClick={() => setExpandidoId(aberto ? null : log.id)}
                              className="text-teal-700 hover:underline font-medium cursor-pointer"
                            >
                              {aberto ? 'Ocultar' : 'Ver JSON'}
                            </button>
                          ) : (
                            <span className="text-slate-400">—</span>
                          )}
                        </td>
                      </tr>
                      {aberto && log.detalhes && (
                        <tr className="bg-slate-50">
                          <td colSpan={6} className="px-4 py-3">
                            <pre className="text-[11px] font-mono text-slate-700 bg-white p-3 rounded-lg border border-slate-200 overflow-x-auto">
                              {JSON.stringify(log.detalhes, null, 2)}
                            </pre>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>

          <div className="px-4 py-3 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
            <span>
              Total: {pageData.totalElements} registros • Página {pageData.number + 1} de{' '}
              {Math.max(1, pageData.totalPages)}
            </span>
            <div className="flex gap-1.5">
              <Button
                size="sm"
                variant="secondary"
                disabled={page <= 0}
                onClick={() => void carregar(page - 1)}
              >
                <ChevronLeft className="w-3.5 h-3.5" /> Anterior
              </Button>
              <Button
                size="sm"
                variant="secondary"
                disabled={page + 1 >= pageData.totalPages}
                onClick={() => void carregar(page + 1)}
              >
                Próxima <ChevronRight className="w-3.5 h-3.5" />
              </Button>
            </div>
          </div>
        </Card>
      )}
    </div>
  );
}
