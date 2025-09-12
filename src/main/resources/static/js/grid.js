window.selectedGridId = null;
window.savedScrollPosition = null;

let GridUtil = {
    changeOrder: function (val, grid) {
        if (!grid || !grid.selectedItems || grid.selectedItems.length === 0) {
            Alert.alert('', '순서를 변경할 Row 를 선택하세요.');
            return;
        }

        let collectionView = grid.collectionView;
        if (!collectionView || !collectionView.items) {
            console.error("CollectionView가 정의되지 않았습니다.");
            return;
        }

        if (val === "U") {
            let items = grid.selectedItems;
            items.forEach((item) => {
                let self_index = collectionView.items.indexOf(item);
                let upper_index = self_index - 1;
                if (upper_index < 0) {
                    Alert.alert('', "첫번째 Row가 선택되었습니다.");
                    return;
                }

                // ✅ 배열에서 항목 위치 변경
                [collectionView.items[self_index], collectionView.items[upper_index]] =
                    [collectionView.items[upper_index], collectionView.items[self_index]];

                collectionView.refresh();
                grid.select(new wijmo.grid.CellRange(upper_index, 0)); // ✅ 수정
            });
        } else {
            let items = grid.selectedItems;
            items.reverse(); // 아래로 내릴 때는 역순으로 루프를 돌려야 한다.
            items.forEach((item) => {
                let self_index = collectionView.items.indexOf(item);
                let under_index = self_index + 1;
                if (under_index >= collectionView.items.length) {
                    Alert.alert('', "마지막 Row가 선택되었습니다.");
                    return;
                }

                // ✅ 배열에서 항목 위치 변경
                [collectionView.items[self_index], collectionView.items[under_index]] =
                    [collectionView.items[under_index], collectionView.items[self_index]];

                collectionView.refresh();
                grid.select(new wijmo.grid.CellRange(under_index, 0)); // ✅ 수정
            });
        }
    },

    adjustHeight: function (grid, rows_len) {
        if (!grid || !grid.hostElement) {
            console.error("Grid가 정의되지 않았습니다.");
            return;
        }
        let rowHeight = grid.rows.defaultSize;
        let headerHeight = grid.columnHeaders.rows.defaultSize;
        let height = headerHeight + (rowHeight * (rows_len + 2));
        if (height < 150) height = 150;
        grid.hostElement.style.height = height + 'px';
    },

    getSelectedId: function (grid, idField = 'id') {
        const it = grid && grid.selectedItems && grid.selectedItems[0];
        return it ? it[idField] : null;
    },

    // === 추가: id로 행 다시 선택(바인딩 타이밍 대응) ===
    // options: { idField='id', focus=true, scroll=true, lastColumnSelect=true }
    selectRowById: function (grid, id, options = {}) {
        const opts = Object.assign({
            idField: 'id',
            focus: true,
            scroll: true,
            lastColumnSelect: true
        }, options);

        if (!grid || id == null || id === '') return;

        const cv = grid.collectionView;

        // 즉시 시도 후 실패하면 collectionChanged 한 번 대기
        if (!this._trySelectRowById(grid, id, opts)) {
            if (cv && cv.collectionChanged) {
                const handler = () => {
                    // 데이터가 바뀐 뒤에 한 번 더 시도
                    setTimeout(() => {
                        if (this._trySelectRowById(grid, id, opts)) {
                            cv.collectionChanged.removeHandler(handler);
                        }
                    }, 0);
                };
                cv.collectionChanged.addHandler(handler);
            } else {
                // collectionView가 없거나 rows가 비어있을 때의 fallback
                setTimeout(() => this._trySelectRowById(grid, id, opts), 0);
            }
        }
    },

    // === 내부: 실제 선택 로직(그룹행 건너뜀) ===
    _trySelectRowById: function (grid, id, opts) {
        if (!grid || !grid.rows || grid.rows.length === 0) return false;

        const lastCol = opts.lastColumnSelect ? Math.max(0, grid.columns.length - 1) : 0;

        for (let r = 0; r < grid.rows.length; r++) {
            const row = grid.rows[r];
            if (row instanceof wijmo.grid.GroupRow) continue;

            const item = row.dataItem;
            if (item && item[opts.idField] == id) {
                // 현재 스크롤 위치 저장
                const prevScroll = grid.scrollPosition;

                // 행 선택 (이때 FlexGrid가 자동 스크롤)
                grid.select(new wijmo.grid.CellRange(r, 0, r, lastCol), true);
                if (opts.focus) grid.focus();

                // scroll 옵션이 false면 이전 스크롤 위치로 복원
                if (opts.scroll === false) {
                    setTimeout(() => {
                        grid.scrollPosition = prevScroll;
                    }, 0);
                }
                return true;
            }
        }
        return false;
    },
    saveView: function (grid, itemOrId, options = {}) {
        const idField = options.idField || 'id';
        let id = null;

        if (itemOrId && typeof itemOrId === 'object') {
            id = itemOrId[idField];
        } else {
            id = itemOrId;
        }

        if (id != null && id !== '') {
            window.selectedGridId = id;
        }
        if (grid && grid.scrollPosition) {
            // 객체 복사(참조깨짐 방지)
            window.savedScrollPosition = new wijmo.Point(grid.scrollPosition.x, grid.scrollPosition.y);
        }
    },
    restoreView: function (grid, options = {}) {
        const idField = options.idField || 'id';
        const clear = options.clear !== false; // 기본 true

        const id = window.selectedGridId;
        const scroll = window.savedScrollPosition;

        // 1) 선택 복원 (스크롤은 건드리지 않음)
        if (id) {
            // selectRowById 내부에서 자동 스크롤 안 하도록 scroll:false
            this.selectRowById(grid, id, { idField, scroll: false });
        }

        // 2) 스크롤 복원 (그리드 내부 자동 스크롤 이후 한 프레임 뒤)
        if (scroll) {
            (window.requestAnimationFrame
                    ? requestAnimationFrame(() => { grid.scrollPosition = scroll; })
                    : setTimeout(() => { grid.scrollPosition = scroll; }, 0)
            );
        }

        if (clear) {
            window.selectedGridId = null;
            window.savedScrollPosition = null;
        }
    },
      enableResponsiveRowSelection(grid, onSelect) {
          if (!grid || !grid.hostElement || typeof onSelect !== 'function') return () => {};

          const host = grid.hostElement;
          const isTouchEnv = window.matchMedia('(hover: none), (pointer: coarse)').matches;

          // 좌표로 행 선택 후 onSelect
          const selectByPoint = (clientX, clientY) => {
              try {
                  const pt = new wijmo.Point(clientX, clientY);
                  const ht = grid.hitTest(pt);
                  if (!ht || ht.cellType !== wijmo.grid.CellType.Cell || ht.row < 0) return false;

                  const lastCol = Math.max(0, grid.columns.length - 1);
                  grid.select(new wijmo.grid.CellRange(ht.row, 0, ht.row, lastCol), true);
                  grid.focus();

                  const item = grid.rows[ht.row]?.dataItem || grid.selectedItems?.[0];
                  if (item) { onSelect(item); return true; }
              } catch (e) {}
              return false;
          };

          // 공통: Enter로 선택
          const onKeyDown = (e) => {
              if (e.key === 'Enter') {
                  const item = grid.selectedItems?.[0];
                  if (item) onSelect(item);
              }
          };
          host.addEventListener('keydown', onKeyDown);

          // 데스크톱: 더블클릭
          let onDblClick = null;
          if (!isTouchEnv) {
              onDblClick = (e) => {
                  // 포인터 위치 기준으로 선택 시도, 실패 시 현재 선택 사용
                  if (!selectByPoint(e.clientX, e.clientY)) {
                      const item = grid.selectedItems?.[0];
                      if (item) onSelect(item);
                  }
              };
              host.addEventListener('dblclick', onDblClick);
              // detach
              return () => {
                  host.removeEventListener('keydown', onKeyDown);
                  if (onDblClick) host.removeEventListener('dblclick', onDblClick);
              };
          }

          // ── 모바일/태블릿: 탭 + 롱프레스 + 클릭 안전망, 버튼바에 안 가리게 높이 보정 ──
          let touchStartY = 0, touchStartX = 0, moved = false, pressTimer = null, justHandled = false;

          // 버튼바 높이만큼 그리드 실제(px) 높이로 보정
          let originalHeight = null, originalMinHeight = null;
          const recomputeGridHeight = () => {
              try {
                  const popupRoot = host.closest('.content_wrap.popup');
                  const container = popupRoot?.querySelector('.container-fluid') || host.parentElement;
                  const btnBar    = popupRoot?.querySelector('.popup-button');
                  if (!container) return;

                  const offset = Math.max(20, btnBar ? btnBar.offsetHeight : 0); // 최소 20px 확보
                  const available = container.clientHeight - offset;
                  const target = Math.max(160, available);

                  if (originalHeight === null) originalHeight = host.style.height || null;
                  if (originalMinHeight === null) originalMinHeight = host.style.minHeight || null;

                  host.style.height    = `${target}px`;
                  host.style.minHeight = `160px`;

                  try { grid.invalidate(); } catch (e) {}
              } catch (e) {}
          };
          // 초기 1회 보정
          setTimeout(recomputeGridHeight, 0);
          // 회전/리사이즈 시 재보정
          const onResize = () => recomputeGridHeight();
          window.addEventListener('resize', onResize);

          const LONGPRESS_MS = 450;
          const TAP_MOVE_TOL = 10;

          const onTouchStart = (e) => {
              moved = false;
              const t = e.touches[0];
              touchStartX = t.clientX; touchStartY = t.clientY;

              clearTimeout(pressTimer);
              pressTimer = setTimeout(() => {
                  // 롱프레스: 시작 좌표 기준
                  if (selectByPoint(touchStartX, touchStartY)) {
                      justHandled = true; setTimeout(() => justHandled = false, 300);
                  }
              }, LONGPRESS_MS);
          };

          const onTouchMove = (e) => {
              const t = e.touches[0];
              if (Math.abs(t.clientX - touchStartX) > TAP_MOVE_TOL ||
                Math.abs(t.clientY - touchStartY) > TAP_MOVE_TOL) {
                  moved = true;
                  clearTimeout(pressTimer);
              }
          };

          const onTouchEnd = (e) => {
              clearTimeout(pressTimer);
              if (moved) return;
              const t = e.changedTouches[0];
              if (selectByPoint(t.clientX, t.clientY)) {
                  justHandled = true; setTimeout(() => justHandled = false, 300);
              }
          };

          // 일부 브라우저는 터치 후 click도 발생 → 안전망
          const onClick = (e) => {
              if (justHandled) return;
              if (!selectByPoint(e.clientX, e.clientY)) {
                  const item = grid.selectedItems?.[0];
                  if (item) onSelect(item);
              }
          };

          host.addEventListener('touchstart', onTouchStart, { passive: true });
          host.addEventListener('touchmove',  onTouchMove,  { passive: true });
          host.addEventListener('touchend',   onTouchEnd);
          host.addEventListener('click',      onClick, true);

          // detach (모달 닫힘/그리드 dispose 시 호출 권장)
          return () => {
              host.removeEventListener('keydown', onKeyDown);
              host.removeEventListener('touchstart', onTouchStart);
              host.removeEventListener('touchmove',  onTouchMove);
              host.removeEventListener('touchend',   onTouchEnd);
              host.removeEventListener('click',      onClick, true);
              window.removeEventListener('resize', onResize);
              clearTimeout(pressTimer);

              // 높이 원복
              if (originalHeight !== null) host.style.height = originalHeight; else host.style.removeProperty('height');
              if (originalMinHeight !== null) host.style.minHeight = originalMinHeight; else host.style.removeProperty('min-height');
          };
      },
    /**
     * (선택) 작은 화면에서 행 앞에 "선택" 버튼 컬럼 추가
     * 이미 추가되어 있으면 건너뜀
     */
    addSelectButtonColumn(grid, onSelect) {
        if (!grid || !grid.columns || typeof onSelect !== 'function') return;
        if (grid.columns.getColumn('___selectBtn')) return; // 중복 방지

        grid.columns.insert(0, new wijmo.grid.Column({
            binding: '___selectBtn',
            header: '',
            width: 64,
            isReadOnly: true,
            align: 'center'
        }));

        // 기존 itemFormatter 보존
        const prevFormatter = grid.itemFormatter;
        grid.itemFormatter = (panel, r, c, cell) => {
            if (prevFormatter) prevFormatter(panel, r, c, cell);
            if (panel.cellType === wijmo.grid.CellType.Cell &&
              panel.columns[c].binding === '___selectBtn') {
                cell.innerHTML = '<button class="wj-row-select-btn" type="button">선택</button>';
                const item = panel.rows[r]?.dataItem;
                const btn = cell.querySelector('.wj-row-select-btn');
                btn.onclick = (e) => {
                    e.stopPropagation();
                    if (item) onSelect(item);
                };
            }
        };
    }
};
