import React, { useEffect, useState } from 'react';
import {
  Box, Paper, Typography, Button, IconButton, Chip, CircularProgress,
  Dialog, DialogTitle, DialogContent, DialogActions, TextField,
  FormControl, InputLabel, Select, MenuItem, Grid, Alert
} from '@mui/material';
import { DataGrid } from '@mui/x-data-grid';
import {
  Add as AddIcon, Edit as EditIcon, Delete as DeleteIcon,
  Refresh as RefreshIcon, Search as SearchIcon, Upload as UploadIcon
} from '@mui/icons-material';
import { migrationsAPI } from '../services/api';
import { useAuth } from '../context/AuthContext';

// Statuts de migration
const MIGRATION_STATUSES = [
  { value: 'PLANIFIE', label: 'Planifié', color: '#FFC107' },
  { value: 'NON_DEMARRE', label: 'Non démarré', color: '#9E9E9E' },
  { value: 'EN_COURS', label: 'En cours', color: '#2196F3' },
  { value: 'TERMINE', label: 'Terminé', color: '#4CAF50' },
  { value: 'ANNULE', label: 'Annulé', color: '#F44336' },
  { value: 'EN_ATTENTE', label: 'En attente', color: '#FF9800' }
];

// Phases par defaut
const DEFAULT_PHASES = ['Phase 1', 'Phase 2', 'Phase 3', 'MCO'];

// Environnements
const ENVIRONMENTS = ['PROD', 'PREPROD', 'RECETTE', 'DEV', 'TEST', 'Tous'];

function MigrationPlanning() {
  const { isAuthenticated } = useAuth();

  const [migrations, setMigrations] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  // Filtres
  const [search, setSearch] = useState('');

  // Pagination
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(50);
  const [totalElements, setTotalElements] = useState(0);

  // Dialog pour ajouter/modifier
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingMigration, setEditingMigration] = useState(null);

  // Import CSV
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importFile, setImportFile] = useState(null);
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState(null);

  const [formData, setFormData] = useState({
    phase: '',
    description: '',
    environmentName: '',
    dateDebut: '',
    dateFin: '',
    responsable: '',
    status: 'PLANIFIE',
    risques: '',
    commentaires: ''
  });

  useEffect(() => {
    loadMigrations();
  }, [page, pageSize]);

  const loadMigrations = async () => {
    try {
      setLoading(true);
      const params = {
        page,
        size: pageSize,
        ...(search && { search })
      };
      const response = await migrationsAPI.getAll(params);
      setMigrations(response.data.content || []);
      setTotalElements(response.data.totalElements || 0);
    } catch (err) {
      console.error('Erreur chargement migrations:', err);
      setError('Erreur lors du chargement des migrations');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    loadMigrations();
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  const openDialog = (migration = null) => {
    if (migration) {
      setEditingMigration(migration);
      setFormData({
        phase: migration.phase || '',
        description: migration.description || '',
        environmentName: migration.environmentName || '',
        dateDebut: migration.dateDebut || '',
        dateFin: migration.dateFin || '',
        responsable: migration.responsable || '',
        status: migration.status || 'PLANIFIE',
        risques: migration.risques || '',
        commentaires: migration.commentaires || ''
      });
    } else {
      setEditingMigration(null);
      setFormData({
        phase: '',
        description: '',
        environmentName: '',
        dateDebut: '',
        dateFin: '',
        responsable: '',
        status: 'PLANIFIE',
        risques: '',
        commentaires: ''
      });
    }
    setDialogOpen(true);
  };

  const handleSubmit = async () => {
    try {
      if (editingMigration) {
        await migrationsAPI.update(editingMigration.id, formData);
      } else {
        await migrationsAPI.create(formData);
      }
      setDialogOpen(false);
      loadMigrations();
    } catch (err) {
      console.error('Erreur sauvegarde:', err);
      alert('Erreur lors de la sauvegarde');
    }
  };

  const handleDelete = async (id) => {
    if (window.confirm('Êtes-vous sûr de vouloir supprimer cette migration ?')) {
      try {
        await migrationsAPI.delete(id);
        loadMigrations();
      } catch (err) {
        console.error('Erreur suppression:', err);
        alert('Erreur lors de la suppression');
      }
    }
  };

  // Import CSV
  const handleImportClick = () => {
    setImportFile(null);
    setImportResult(null);
    setImportDialogOpen(true);
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      setImportFile(file);
      setImportResult(null);
    }
  };

  const handleImport = async () => {
    if (!importFile) return;

    try {
      setImporting(true);
      const response = await migrationsAPI.importCsv(importFile);
      setImportResult(response.data);
      loadMigrations();
    } catch (err) {
      console.error('Erreur import:', err);
      setImportResult({
        error: true,
        message: err.response?.data?.message || 'Erreur lors de l\'import'
      });
    } finally {
      setImporting(false);
    }
  };

  const getStatusChip = (status) => {
    const statusInfo = MIGRATION_STATUSES.find(s => s.value === status);
    if (!statusInfo) return <Chip label={status} size="small" />;
    return (
      <Chip
        label={statusInfo.label}
        size="small"
        sx={{
          backgroundColor: statusInfo.color,
          color: statusInfo.color === '#FFC107' || statusInfo.color === '#FF9800' ? '#000' : '#fff',
          fontWeight: 'bold'
        }}
      />
    );
  };

  const getEnvColor = (env) => {
    switch (env?.toUpperCase()) {
      case 'PROD': return 'error';
      case 'PREPROD': return 'warning';
      case 'RECETTE': return 'info';
      case 'DEV': return 'success';
      case 'TOUS': return 'default';
      default: return 'default';
    }
  };

  const getPhaseColor = (phase) => {
    if (phase?.toLowerCase().includes('phase 1')) return '#1976D2';
    if (phase?.toLowerCase().includes('phase 2')) return '#388E3C';
    if (phase?.toLowerCase().includes('phase 3')) return '#F57C00';
    if (phase?.toLowerCase().includes('mco')) return '#7B1FA2';
    return '#616161';
  };

  const columns = [
    {
      field: 'phase',
      headerName: 'Phase',
      width: 100,
      renderCell: (params) => (
        <Chip
          label={params.value}
          size="small"
          sx={{
            backgroundColor: getPhaseColor(params.value),
            color: '#fff',
            fontWeight: 'bold'
          }}
        />
      )
    },
    {
      field: 'description',
      headerName: 'Description',
      flex: 1,
      minWidth: 300
    },
    {
      field: 'environmentName',
      headerName: 'Environnement',
      width: 120,
      renderCell: (params) => (
        params.value ? (
          <Chip
            label={params.value}
            color={getEnvColor(params.value)}
            size="small"
          />
        ) : '-'
      )
    },
    {
      field: 'dateDebut',
      headerName: 'Date début',
      width: 110,
      renderCell: (params) => {
        if (!params.value) return '-';
        if (params.value === 'Continu' || params.value === 'Mensuel') return params.value;
        try {
          return new Date(params.value).toLocaleDateString('fr-FR');
        } catch {
          return params.value;
        }
      }
    },
    {
      field: 'dateFin',
      headerName: 'Date fin',
      width: 110,
      renderCell: (params) => {
        if (!params.value) return '-';
        if (params.value === 'Continu' || params.value === 'Mensuel') return params.value;
        try {
          return new Date(params.value).toLocaleDateString('fr-FR');
        } catch {
          return params.value;
        }
      }
    },
    {
      field: 'responsable',
      headerName: 'Responsable',
      width: 130,
      renderCell: (params) => params.value || '-'
    },
    {
      field: 'status',
      headerName: 'Statut',
      width: 130,
      renderCell: (params) => getStatusChip(params.value)
    },
    {
      field: 'risques',
      headerName: 'Risques/Commentaires',
      flex: 1,
      minWidth: 200,
      renderCell: (params) => (
        <Typography variant="body2" noWrap title={params.value}>
          {params.value || params.row.commentaires || '-'}
        </Typography>
      )
    },
    {
      field: 'actions',
      headerName: 'Actions',
      width: 100,
      sortable: false,
      renderCell: (params) => (
        isAuthenticated() && (
          <Box>
            <IconButton size="small" onClick={() => openDialog(params.row)} title="Modifier">
              <EditIcon fontSize="small" />
            </IconButton>
            <IconButton size="small" color="error" onClick={() => handleDelete(params.row.id)} title="Supprimer">
              <DeleteIcon fontSize="small" />
            </IconButton>
          </Box>
        )
      )
    }
  ];

  return (
    <Box>
      {/* Header */}
      <Box display="flex" justifyContent="space-between" alignItems="center" mb={3}>
        <Typography variant="h5" fontWeight="bold" color="primary">
          PLANNING DES MIGRATIONS GED-PEI
        </Typography>
        {isAuthenticated() && (
          <Box display="flex" gap={1}>
            <Button
              variant="outlined"
              startIcon={<UploadIcon />}
              onClick={handleImportClick}
            >
              Importer CSV
            </Button>
            <Button
              variant="contained"
              startIcon={<AddIcon />}
              onClick={() => openDialog()}
            >
              Ajouter une migration
            </Button>
          </Box>
        )}
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Filtres */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" gap={2} flexWrap="wrap" alignItems="center">
          <TextField
            size="small"
            placeholder="Rechercher..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            onKeyPress={handleKeyPress}
            InputProps={{
              startAdornment: <SearchIcon sx={{ mr: 1, color: 'text.secondary' }} />
            }}
            sx={{ minWidth: 300 }}
          />
          <Button variant="outlined" onClick={handleSearch}>
            Rechercher
          </Button>
          <IconButton onClick={loadMigrations}>
            <RefreshIcon />
          </IconButton>
        </Box>
      </Paper>

      {/* Tableau */}
      <Paper sx={{ height: 600, width: '100%' }}>
        <DataGrid
          rows={migrations}
          columns={columns}
          loading={loading}
          pageSizeOptions={[10, 25, 50, 100]}
          paginationModel={{ page, pageSize }}
          onPaginationModelChange={(model) => {
            setPage(model.page);
            setPageSize(model.pageSize);
          }}
          rowCount={totalElements}
          paginationMode="server"
          disableRowSelectionOnClick
          sx={{
            '& .MuiDataGrid-cell:focus': { outline: 'none' },
            '& .MuiDataGrid-row:hover': { bgcolor: 'action.hover' },
            '& .MuiDataGrid-columnHeaders': {
              backgroundColor: '#1976D2',
              color: '#fff',
              fontWeight: 'bold'
            }
          }}
        />
      </Paper>

      {/* Dialog pour ajouter/modifier */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>
          {editingMigration ? 'Modifier la migration' : 'Ajouter une migration'}
        </DialogTitle>
        <DialogContent>
          <Grid container spacing={2} sx={{ mt: 1 }}>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Phase</InputLabel>
                <Select
                  value={formData.phase}
                  label="Phase"
                  onChange={(e) => setFormData({ ...formData, phase: e.target.value })}
                >
                  {DEFAULT_PHASES.map(p => (
                    <MenuItem key={p} value={p}>{p}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Environnement</InputLabel>
                <Select
                  value={formData.environmentName}
                  label="Environnement"
                  onChange={(e) => setFormData({ ...formData, environmentName: e.target.value })}
                >
                  <MenuItem value="">-- Sélectionner --</MenuItem>
                  {ENVIRONMENTS.map(env => (
                    <MenuItem key={env} value={env}>{env}</MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                required
                label="Description"
                value={formData.description}
                onChange={(e) => setFormData({ ...formData, description: e.target.value })}
                placeholder="Ex: Migration NAS anciens vers nouveaux volumes"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="date"
                label="Date début"
                value={formData.dateDebut}
                onChange={(e) => setFormData({ ...formData, dateDebut: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                type="date"
                label="Date fin"
                value={formData.dateFin}
                onChange={(e) => setFormData({ ...formData, dateFin: e.target.value })}
                InputLabelProps={{ shrink: true }}
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                fullWidth
                label="Responsable"
                value={formData.responsable}
                onChange={(e) => setFormData({ ...formData, responsable: e.target.value })}
                placeholder="Ex: Admin Storage, DBA, Chef projet"
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <FormControl fullWidth>
                <InputLabel>Statut</InputLabel>
                <Select
                  value={formData.status}
                  label="Statut"
                  onChange={(e) => setFormData({ ...formData, status: e.target.value })}
                >
                  {MIGRATION_STATUSES.map(s => (
                    <MenuItem key={s.value} value={s.value}>
                      <Box display="flex" alignItems="center" gap={1}>
                        <Box
                          sx={{
                            width: 12,
                            height: 12,
                            borderRadius: '50%',
                            backgroundColor: s.color
                          }}
                        />
                        {s.label}
                      </Box>
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid item xs={12}>
              <TextField
                fullWidth
                multiline
                rows={2}
                label="Risques / Commentaires"
                value={formData.risques || formData.commentaires}
                onChange={(e) => setFormData({ ...formData, risques: e.target.value })}
                placeholder="Ex: Prévoir fenêtre maintenance, Test avant PROD"
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Annuler</Button>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={!formData.phase || !formData.description}
          >
            {editingMigration ? 'Modifier' : 'Ajouter'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* Dialog pour import CSV */}
      <Dialog open={importDialogOpen} onClose={() => setImportDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Importer des migrations depuis un fichier CSV</DialogTitle>
        <DialogContent>
          <Box sx={{ mt: 2 }}>
            <Typography variant="body2" color="text.secondary" gutterBottom>
              Format attendu : fichier CSV avec les colonnes Phase, Description, Environnement,
              Date début, Date fin, Responsable, Statut, Risques/Commentaires (séparateur: point-virgule)
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              Les migrations existantes (même phase + description + environnement) seront mises à jour.
              Les nouvelles seront créées.
            </Typography>

            <Button
              variant="outlined"
              component="label"
              fullWidth
              sx={{ mb: 2 }}
            >
              {importFile ? importFile.name : 'Sélectionner un fichier CSV'}
              <input
                type="file"
                hidden
                accept=".csv"
                onChange={handleFileChange}
              />
            </Button>

            {importResult && !importResult.error && (
              <Alert severity="success" sx={{ mb: 2 }}>
                Import terminé : {importResult.created} créées, {importResult.updated} mises à jour
                {importResult.errors > 0 && `, ${importResult.errors} erreurs`}
              </Alert>
            )}

            {importResult && importResult.error && (
              <Alert severity="error" sx={{ mb: 2 }}>
                {importResult.message}
              </Alert>
            )}
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportDialogOpen(false)}>
            {importResult ? 'Fermer' : 'Annuler'}
          </Button>
          {!importResult && (
            <Button
              variant="contained"
              onClick={handleImport}
              disabled={!importFile || importing}
              startIcon={importing ? <CircularProgress size={20} /> : <UploadIcon />}
            >
              {importing ? 'Import en cours...' : 'Importer'}
            </Button>
          )}
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default MigrationPlanning;
