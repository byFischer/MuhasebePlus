import React, { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import BeyannameTabView from './components/BeyannameTabView';
import declarationService from '@/services/declarationService';
import {
  useVatDeclarations, useGenerateVatDeclaration, useFinalizeVatDeclaration,
  useSubmitVatDeclaration, useDeleteVatDeclaration,
  useBaBsDeclarations, useGenerateBaBsDeclaration, useFinalizeBaBsDeclaration, useDeleteBaBsDeclaration,
  useWithholdingDeclarations, useGenerateWithholdingDeclaration, useFinalizeWithholdingDeclaration, useDeleteWithholdingDeclaration,
  useGenericDeclarations, useGenerateGenericDeclaration, useFinalizeGenericDeclaration,
  useSubmitGenericDeclaration, useDeleteGenericDeclaration,
} from '@/hooks/useDeclarations';

const TABS = [
  { key: 'kdv1',     label: 'KDV1',              type: 'KDV1',           periodType: 'MONTH'   },
  { key: 'kdv2',     label: 'KDV2',              type: 'KDV2',           periodType: 'MONTH'   },
  { key: 'muhtasar', label: 'Muhtasar',          type: 'MUHTASAR',       periodType: 'MONTH'   },
  { key: 'babs',     label: 'Ba-Bs',             type: 'BABS',           periodType: 'MONTH'   },
  { key: 'gecici',   label: 'Geçici Vergi',      type: 'TEMPORARY_TAX',  periodType: 'QUARTER' },
  { key: 'kurumlar', label: 'Kurumlar Vergisi',  type: 'CORPORATE_TAX',  periodType: 'YEAR'    },
];

function Kdv1Tab() {
  const { data = [], isLoading } = useVatDeclarations();
  const generate  = useGenerateVatDeclaration();
  const finalize  = useFinalizeVatDeclaration();
  const submit    = useSubmitVatDeclaration();
  const remove    = useDeleteVatDeclaration();

  return (
    <BeyannameTabView
      declarations={data}
      isLoading={isLoading}
      periodType="MONTH"
      typeName="KDV1"
      declarationType="KDV1"
      onGenerate={({ year, month }, opts) => generate.mutate({ year, month }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onSubmit={(id) => submit.mutate(id)}
      isSubmitPending={submit.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadXml: declarationService.vat.downloadXml, downloadExcel: declarationService.vat.downloadExcel }}
    />
  );
}

function Kdv2Tab() {
  const { data = [], isLoading } = useGenericDeclarations('KDV2');
  const generate  = useGenerateGenericDeclaration();
  const finalize  = useFinalizeGenericDeclaration();
  const submit    = useSubmitGenericDeclaration();
  const remove    = useDeleteGenericDeclaration();

  return (
    <BeyannameTabView
      declarations={data}
      isLoading={isLoading}
      periodType="MONTH"
      typeName="KDV2"
      declarationType="KDV2"
      onGenerate={({ year, month }, opts) => generate.mutate({ type: 'KDV2', year, period: month }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onSubmit={(id) => submit.mutate(id)}
      isSubmitPending={submit.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadExcel: declarationService.generic.downloadExcel }}
    />
  );
}

function MuhtasarTab() {
  const { data = [], isLoading } = useWithholdingDeclarations();
  const generate  = useGenerateWithholdingDeclaration();
  const finalize  = useFinalizeWithholdingDeclaration();
  const remove    = useDeleteWithholdingDeclaration();

  return (
    <BeyannameTabView
      declarations={data}
      isLoading={isLoading}
      periodType="MONTH"
      typeName="Muhtasar"
      declarationType="MUHTASAR"
      onGenerate={({ year, month }, opts) => generate.mutate({ year, month }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadExcel: declarationService.withholding.downloadExcel }}
    />
  );
}

function BaBsTab() {
  const [babsType, setBabsType] = useState('BS');
  const { data = [], isLoading } = useBaBsDeclarations();
  const filtered = data.filter(d => d.babsType === babsType);
  const generate  = useGenerateBaBsDeclaration();
  const finalize  = useFinalizeBaBsDeclaration();
  const remove    = useDeleteBaBsDeclaration();

  return (
    <BeyannameTabView
      declarations={filtered}
      isLoading={isLoading}
      periodType="MONTH"
      typeName={babsType === 'BA' ? 'Ba Formu (Alış)' : 'Bs Formu (Satış)'}
      declarationType={babsType}
      onGenerate={({ year, month }, opts) => generate.mutate({ year, month, type: babsType }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadExcel: declarationService.babs.downloadExcel }}
      extraToolbar={
        <select className="input" style={{ width: 140 }} value={babsType} onChange={e => setBabsType(e.target.value)}>
          <option value="BS">Bs (Satış)</option>
          <option value="BA">Ba (Alış)</option>
        </select>
      }
    />
  );
}

function GeciciVergiTab() {
  const { data = [], isLoading } = useGenericDeclarations('TEMPORARY_TAX');
  const generate  = useGenerateGenericDeclaration();
  const finalize  = useFinalizeGenericDeclaration();
  const remove    = useDeleteGenericDeclaration();

  return (
    <BeyannameTabView
      declarations={data}
      isLoading={isLoading}
      periodType="QUARTER"
      typeName="Geçici Vergi"
      declarationType="GECICI_VERGI"
      onGenerate={({ year, period }, opts) => generate.mutate({ type: 'TEMPORARY_TAX', year, period }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadExcel: declarationService.generic.downloadExcel }}
    />
  );
}

function KurumlarTab() {
  const { data = [], isLoading } = useGenericDeclarations('CORPORATE_TAX');
  const generate  = useGenerateGenericDeclaration();
  const finalize  = useFinalizeGenericDeclaration();
  const submit    = useSubmitGenericDeclaration();
  const remove    = useDeleteGenericDeclaration();

  return (
    <BeyannameTabView
      declarations={data}
      isLoading={isLoading}
      periodType="YEAR"
      typeName="Kurumlar Vergisi"
      declarationType="KURUMLAR_VERGISI"
      onGenerate={({ year }, opts) => generate.mutate({ type: 'CORPORATE_TAX', year, period: 0 }, opts)}
      isGeneratePending={generate.isPending}
      onFinalize={(id) => finalize.mutate(id)}
      isFinalizePending={finalize.isPending}
      onSubmit={(id) => submit.mutate(id)}
      isSubmitPending={submit.isPending}
      onDelete={(id) => remove.mutate(id)}
      isDeletePending={remove.isPending}
      downloadService={{ downloadExcel: declarationService.generic.downloadExcel }}
    />
  );
}

const TAB_COMPONENTS = {
  kdv1:     Kdv1Tab,
  kdv2:     Kdv2Tab,
  muhtasar: MuhtasarTab,
  babs:     BaBsTab,
  gecici:   GeciciVergiTab,
  kurumlar: KurumlarTab,
};

export default function BeyannamePage() {
  const [params, setParams] = useSearchParams();
  const activeTab = params.get('tab') || 'kdv1';
  const setTab = (key) => setParams({ tab: key });

  const ActiveComponent = TAB_COMPONENTS[activeTab] || Kdv1Tab;

  return (
    <div className="page">
      <div className="page-head">
        <div>
          <h1 className="page-title">Beyannameler</h1>
          <p className="page-sub">KDV, Muhtasar, Ba-Bs, Geçici Vergi ve Kurumlar Vergisi beyanname taslakları</p>
        </div>
      </div>

      <div className="card">
        <div className="tabs" style={{ overflowX: 'auto' }}>
          {TABS.map(t => (
            <button key={t.key} className={activeTab === t.key ? 'on' : ''} onClick={() => setTab(t.key)}>
              {t.label}
            </button>
          ))}
        </div>
        <div className="card-b">
          <ActiveComponent />
        </div>
      </div>
    </div>
  );
}
